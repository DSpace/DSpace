/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

/**
 * Stores, retrieves and deletes bitstreams.
 *
 * Presently, asset stores are specified in <code>dspace.cfg</code>. Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property. The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and DSpace (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.
 *
 * @author Peter Breton, Robert Tansley, David Little, Nathan Sarr, Ryan Scherle
 * @version $Revision$
 */
public class BitstreamStorageManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);

    /**
     * The filesystem asset store locations. The information for each
     * path in the array comes from dspace.cfg, so see the comments
     * in that file.
     *
     * Most array elements refer to a conventional filesystem asset store,
     * using a path for a local directory.
     */
    private static File[] assetStores;

    /** The asset store number to use for new bitstreams.
        The default is 0. 
     */
    private static int incoming;

    // These settings control the way an identifier is hashed into
    // directory and file names
    //
    // With digitsPerLevel 2 and directoryLevels 3, an identifier
    // like 12345678901234567890 turns into the relative name
    // /12/34/56/12345678901234567890.
    //
    // You should not change these settings if you have data in the
    // asset store, as the BitstreamStorageManager will be unable
    // to find your existing data.
    private static final int digitsPerLevel = 2;

    private static final int directoryLevels = 3;

    /**
     * This prefix string marks registered bitstreams in internal_id
     */
    private static final String REGISTERED_FLAG = "-R";

    /** Amazon S3 configuration */
    private static final String CSA = "MD5";
    private static final int S3_ASSETSTORE = 1;
    private static boolean s3Enabled = false;
    private static String awsAccessKey;
    private static String awsSecretKey;
    private static String awsRegionName;
    private static String s3BucketName = null;
    // (Optional) subfolder within bucket where objects are stored 
    private static String s3Subfolder = null;

    /** S3 service */
    private static AmazonS3 s3Service = null;

    
    /* Read in the asset stores from the config. */
    static
    {
        List<Object> stores = new ArrayList<Object>();

        // 'assetstore.dir' is always store number 0
        String sAssetstoreDir = ConfigurationManager.getProperty("assetstore.dir");
        
        if (sAssetstoreDir != null) {
            stores.add(sAssetstoreDir);
        } else {
            log.error("No default assetstore");
        }

        // AWS S3 configuration
        awsAccessKey = ConfigurationManager.getProperty("aws.accessKey");
        if (awsAccessKey != null && awsAccessKey.length() > 0) {
            s3Enabled = true;
            log.info("Amazon S3 configuration found (assetstore 1)");
            awsSecretKey = ConfigurationManager.getProperty("aws.secretKey");
            awsRegionName = ConfigurationManager.getProperty("aws.regionName");
            s3BucketName = ConfigurationManager.getProperty("aws.s3.bucketName");
            s3Subfolder = ConfigurationManager.getProperty("aws.s3.subfolder");
            try {
                initS3();
            } catch (Exception e) {
                log.error("Unable to initializes S3 storage");
            }
        }
        // Add a reserved entry to the list of stores regardless of whether S3 is configured
        stores.add("AWS S3 assetstore " + S3_ASSETSTORE);
        
        // read in assetstores .2, .3, ....
        for (int i = 2;; i++) { // i == 0 is default above, i == 1 is reserved for S3
            sAssetstoreDir = ConfigurationManager.getProperty("assetstore.dir." + i);
            
            if (sAssetstoreDir != null) { 		// conventional (non-srb)
                stores.add(sAssetstoreDir);
            } else {
                break; // must be at the end of the assetstores
            }
        }

        // convert list to array
        assetStores = new File[stores.size()];
        for (int i = 0; i < stores.size(); i++) {
            Object o = stores.get(i);
            if (i == S3_ASSETSTORE) {
                // do nothing, since S3 is configured elsewhere
                continue;
            }
            if (o == null) { // I don't know if this can occur
                log.error("Problem with assetstore " + i);
            }
            if (o instanceof String) {
                assetStores[i] = new File((String) o);
            }
        }

        // Read asset store to put new files in. Default is 0.
        incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
    }
  
    
    /**
     * Initialize an S3 asset store
     * S3 Requires:
     *  - access key
     *  - secret key
     *  - bucket name
     */
    private static void initS3() throws IOException {
        if (getAwsAccessKey() == null || getAwsAccessKey().length() == 0 ||
            getAwsSecretKey() == null || getAwsSecretKey().length() == 0) {
            log.warn("Empty S3 access or secret");
        }

        // init client
        AWSCredentials awsCredentials = new BasicAWSCredentials(getAwsAccessKey(), getAwsSecretKey());
        s3Service = new AmazonS3Client(awsCredentials);

        // bucket name
        if (s3BucketName == null || s3BucketName.length() == 0) {
            s3BucketName = "dspace-asset-" + ConfigurationManager.getProperty("dspace.hostname");
            log.warn("S3 BucketName is not configured, setting default: " + s3BucketName);
        }

        // region
        if (awsRegionName != null && awsRegionName.length() > 0) {
            try {
                Regions regions = Regions.fromName(awsRegionName);
                Region region = Region.getRegion(regions);
                s3Service.setRegion(region);
                log.info("S3 Region set to: " + region.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid aws_region: " + awsRegionName);
            }
        }

        try {
            if (! s3Service.doesBucketExist(s3BucketName)) {
                s3Service.createBucket(s3BucketName);
                log.info("Creating new S3 Bucket: " + s3BucketName);
            }
        }
        catch (Exception e)
        {
            log.error(e);
            throw new IOException(e);
        }

        log.info("AWS S3 Assetstore ready to go! bucket:" + s3BucketName);
    }

    public static String getAwsAccessKey() {
        return awsAccessKey;
    }
    
    public static void setAwsAccessKey(String awsAccessKey) {
        BitstreamStorageManager.awsAccessKey = awsAccessKey;
    }

    public static String getAwsSecretKey() {
        return awsSecretKey;
    }
    
    public static void setAwsSecretKey(String awsSecretKey) {
        BitstreamStorageManager.awsSecretKey = awsSecretKey;
    }

    public static String getAwsRegionName() {
        return awsRegionName;
    }
    
    public static void setAwsRegionName(String awsRegionName) {
        BitstreamStorageManager.awsRegionName = awsRegionName;
    }
    
    public static String getS3BucketName() {
        return s3BucketName;
    }
    
    public static void setS3BucketName(String s3BucketName) {
        BitstreamStorageManager.s3BucketName = s3BucketName;
    }
    
    public static String getS3Subfolder() {
        return s3Subfolder;
    }
    
    public static void setS3Subfolder(String s3Subfolder) {
        BitstreamStorageManager.s3Subfolder = s3Subfolder;
    }

    /**
     * Utility Method: Prefix the key with a subfolder, if this instance assets
     * are stored within subfolder
     * @param id
     *     DSpace bitstream internal ID
     * @return full key prefixed with a subfolder, if applicable
     */
    public static String getFullS3Key(String id) {
        if (s3Subfolder != null && s3Subfolder.length() > 0) {
            return s3Subfolder + "/" + id;
        } else {
            return id;
        }
    }
    
    /**
     * Store a stream of bits.
     * 
     * <p>
     * If this method returns successfully, the bits have been stored, and RDBMS
     * metadata entries are in place (the context still needs to be completed to
     * finalize the transaction).
     * </p>
     * 
     * <p>
     * If this method returns successfully and the context is aborted, then the
     * bits will be stored in the asset store and the RDBMS metadata entries
     * will exist, but with the deleted flag set.
     * </p>
     * 
     * If this method throws an exception, then any of the following may be
     * true:
     * 
     * <ul>
     * <li>Neither bits nor RDBMS metadata entries have been stored.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * but no bits.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * and some or all of the bits have also been stored.
     * </ul>
     * 
     * @param context
     *            The current context
     * @param is
     *            The stream of bits to store
     * @exception IOException
     *                If a problem occurs while storing the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The ID of the stored bitstream
     */
    public static int store(Context context, InputStream is)
            throws SQLException, IOException
    {
        // Create internal ID
        String id = Utils.generateKey();

        // Create a deleted bitstream row, using a separate DB connection
        TableRow bitstream;
        Context tempContext = null;
        
        try
        {
            tempContext = new Context();

            bitstream = DatabaseManager.row("Bitstream");
            bitstream.setColumn("deleted", true);
            bitstream.setColumn("internal_id", id);

            /*
             * Set the store number of the new bitstream If you want to use some
             * other method of working out where to put a new bitstream, here's
             * where it should go
             */
            bitstream.setColumn("store_number", incoming);

            DatabaseManager.insert(tempContext, bitstream);

            tempContext.complete();
        }
        catch (SQLException sqle)
        {
            if (tempContext != null)
            {
                tempContext.abort();
            }

            throw sqle;
        }

        String storedLocation;
        
        if(incoming == S3_ASSETSTORE) {
            String key = getFullS3Key(id);
            //Copy input stream to temp file, and send the file to S3 with some metadata
            File scratchFile = File.createTempFile(id, "s3bs");
            try {
                FileUtils.copyInputStreamToFile(is, scratchFile);
                Long contentLength = Long.valueOf(scratchFile.length());
                PutObjectRequest putObjectRequest = new PutObjectRequest(s3BucketName, key, scratchFile);
                PutObjectResult putObjectResult = s3Service.putObject(putObjectRequest);

                bitstream.setColumn("checksum", putObjectResult.getETag());
                bitstream.setColumn("checksum_algorithm", CSA);
                bitstream.setColumn("size_bytes", contentLength);
                scratchFile.delete();
                storedLocation = "Amazon S3 " + getS3BucketName() + ":" + key;
            } catch(Exception e) {
                log.error("Unable to store " + id + " in S3 bucket " + s3BucketName, e);
                throw new IOException(e);
            } finally {
                if (scratchFile.exists()) {
                    scratchFile.delete();
                }
            }
        } else {
            // Store bitstream in a local filesystem
            // Where on the file system will this new bitstream go?
            File file = getFile(bitstream);
            
            // Make the parent dirs if necessary
            File parent = file.getParentFile();

            if (!parent.exists()) {
                parent.mkdirs();
            }
            
            // Create the corresponding file and open it
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);

            // Read through a digest input stream that will work out the MD5
            DigestInputStream dis = null;

            try {
                dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
            } catch (NoSuchAlgorithmException nsae) {
                // Should never happen
                log.warn("Caught NoSuchAlgorithmException", nsae);
            }

            Utils.bufferedCopy(dis, fos);
            fos.close();
            is.close();

            if (dis != null) {
                bitstream.setColumn("checksum", Utils.toHex(dis.getMessageDigest()
                                                            .digest()));
                bitstream.setColumn("checksum_algorithm", "MD5");
                bitstream.setColumn("size_bytes", file.length());
            }

            storedLocation = file.getAbsolutePath();
        }
        
        bitstream.setColumn("deleted", false);
        DatabaseManager.update(context, bitstream);

        int bitstreamId = bitstream.getIntColumn("bitstream_id");
        if (log.isDebugEnabled()) {
            log.debug("Stored bitstream " + bitstreamId + " in file "
                      + storedLocation);
        }

        return bitstreamId;
    }

    
    /**
	 * Register a bitstream already in storage.
	 *
	 * @param context
	 *            The current context
	 * @param assetstore The assetstore number for the bitstream to be
	 * 			registered
	 * @param bitstreamPath The relative path of the bitstream to be registered.
	 * 		The path is relative to the path of ths assetstore.
	 * @return The ID of the registered bitstream
	 * @exception SQLException
	 *                If a problem occurs accessing the RDBMS
	 * @throws IOException
	 */
	public static int register(Context context, int assetstore,
				String bitstreamPath) throws SQLException, IOException {

                // Don't allow bitstreams to be registered in an Amazon S3 Assetstore
                if(assetstore == S3_ASSETSTORE) {
                    throw new IOException("Registration of bitstreams in Amazon S3 is not supported.");
                }
            
		// mark this bitstream as a registered bitstream
		String sInternalId = REGISTERED_FLAG + bitstreamPath;

		// Create a deleted bitstream row, using a separate DB connection
		TableRow bitstream;
		Context tempContext = null;
                
		try {
			tempContext = new Context();

			bitstream = DatabaseManager.row("Bitstream");
			bitstream.setColumn("deleted", true);
			bitstream.setColumn("internal_id", sInternalId);
			bitstream.setColumn("store_number", assetstore);
			DatabaseManager.insert(tempContext, bitstream);

			tempContext.complete();
		} catch (SQLException sqle) {
			if (tempContext != null) {
				tempContext.abort();
			}
			throw sqle;
		}

		// get a reference to the file
		File file = getFile(bitstream);

		// read through a DigestInputStream that will work out the MD5
		//
		// DSpace refers to checksum, writes it in METS, and uses it as an
		// AIP filename (!), but never seems to validate with it. Furthermore,
		// DSpace appears to hardcode the algorithm to MD5 in some places--see 
		// METSExport.java.
		//
		// TODO set this up as a proxy server process so no net activity
		
		// FIXME this is a first class HACK! for the reasons described above

                // get MD5 on the file for local file
                DigestInputStream dis = null;
                try {
                    dis = new DigestInputStream(new FileInputStream(file),
                                                MessageDigest.getInstance("MD5"));
                } catch (NoSuchAlgorithmException e) {
                    log.warn("Caught NoSuchAlgorithmException", e);
                    throw new IOException("Invalid checksum algorithm", e);
                } catch (IOException e) {
                    log.error("File: " + file.getAbsolutePath() 
                              + " to be registered cannot be opened - is it "
                              + "really there?");
                    throw e;
                }

                final int BUFFER_SIZE = 1024 * 4;
                final byte[] buffer = new byte[BUFFER_SIZE];
                while (true) {
                    final int count = dis.read(buffer, 0, BUFFER_SIZE);
                    if (count == -1) {
                        break;
                    }
                }
                bitstream.setColumn("checksum", Utils.toHex(dis.getMessageDigest()
                                                            .digest()));
                dis.close();
                
		bitstream.setColumn("checksum_algorithm", "MD5");
		bitstream.setColumn("size_bytes", file.length());
		bitstream.setColumn("deleted", false);
		DatabaseManager.update(context, bitstream);
                
		int bitstreamId = bitstream.getIntColumn("bitstream_id");
		if (log.isDebugEnabled()) {
                    log.debug("Stored bitstream " + bitstreamId + " in file "
                              + file.getAbsolutePath());
                }
		return bitstreamId;
	}

	/**
	 * Does the internal_id column in the bitstream row indicate the bitstream
	 * is a registered file
	 *
	 * @param internalId the value of the internal_id column
	 * @return true if the bitstream is a registered file
	 */
	public static boolean isRegisteredBitstream(String internalId) {
	    if (internalId.substring(0, REGISTERED_FLAG.length())
	            .equals(REGISTERED_FLAG)) 
	    {
	        return true;
	    }
	    return false;
	}

    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream does not
     * exist, or is marked deleted, returns null.
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to retrieve
     * @exception IOException
     *                If a problem occurs while retrieving the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id)
            throws SQLException, IOException
    {
        InputStream resultInputStream = null;
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        int storeNumber = bitstream.getIntColumn("store_number");
        String sInternalId = bitstream.getStringColumn("internal_id");
        
        if(storeNumber == S3_ASSETSTORE) {
            String key = getFullS3Key(sInternalId + "");
            log.debug("retrieving item " + key + " from Amazon S3 bucket " + s3BucketName);
            try {
                //get tomorrow's date:
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                URL url = s3Service.generatePresignedUrl(s3BucketName, key, calendar.getTime());
                resultInputStream = url.openStream();
            } catch (Exception e) {
                log.error("Unable to get S3 item " + key + " from bucket " + s3BucketName, e);
                throw new IOException(e);
            }
        } else {
            // retrieve from local file storage
            File file = getFile(bitstream);
            resultInputStream = (file != null) ? new FileInputStream(file) : null;
        }
        
        return resultInputStream;
    }

    /**
     * <p>
     * Remove a bitstream from the asset store. This method does not delete any
     * bits, but simply marks the bitstreams as deleted (the context still needs
     * to be completed to finalize the transaction).
     * </p>
     * 
     * <p>
     * If the context is aborted, the bitstreams deletion status remains
     * unchanged.
     * </p>
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to delete
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public static void delete(Context context, int id) throws SQLException
    {
        DatabaseManager.updateQuery(context,
                "update Bundle set primary_bitstream_id=null where primary_bitstream_id = ? ",
                id);

        DatabaseManager.updateQuery(context,
                        "update Bitstream set deleted = '1' where bitstream_id = ? ",
                        id);
    }

    /**
     * Clean up the bitstream storage area. This method deletes any bitstreams
     * which are more than 1 hour old and marked deleted. The deletions cannot
     * be undone.
     * 
     * @param deleteDbRecords if true deletes the database records otherwise it
     * 	           only deletes the files and directories in the assetstore  
     * @exception IOException
     *                If a problem occurs while cleaning up
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public static void cleanup(boolean deleteDbRecords, boolean verbose)
        throws SQLException, IOException {
        Context context = null;
        BitstreamInfoDAO bitstreamInfoDAO = new BitstreamInfoDAO();
        int commitCounter = 0;

        try
        {
            context = new Context();

            String myQuery = "select * from Bitstream where deleted = '1'";

            List<TableRow> storage = DatabaseManager.queryTable(context, "Bitstream", myQuery)
                    .toList();

            for (Iterator<TableRow> iterator = storage.iterator(); iterator.hasNext();)
            {
                TableRow row = iterator.next();
                int bid = row.getIntColumn("bitstream_id");

                File file = getFile(row);

                // Make sure entries which do not exist are removed
                if (file == null || !file.exists())
                {
                    log.debug("file is null");
                    if (deleteDbRecords)
                    {
                        log.debug("deleting record");
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                        }
                        bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                        }
                        DatabaseManager.delete(context, "Bitstream", bid);
                    }
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                if (isRecent(file)) {
                    log.debug("file is recent");
                    continue;
                }

                if (deleteDbRecords)
                {
                    log.debug("deleting db record");
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                    }
                    bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                    }
                    DatabaseManager.delete(context, "Bitstream", bid);
                }

				if (isRegisteredBitstream(row.getStringColumn("internal_id"))) {
				    continue;			// do not delete registered bitstreams
				}

                boolean success = file.delete();

                String message = ("Deleted bitstream " + bid + " (file "
                            + file.getAbsolutePath() + ") with result "
                            + success);
                if (log.isDebugEnabled())
                {
                    log.debug(message);
                }
                if (verbose)
                {
                    System.out.println(message);
                }

                // if the file was deleted then
                // try deleting the parents
                // Otherwise the cleanup script is set to 
                // leave the db records then the file
                // and directories have already been deleted
                // if this is turned off then it still looks like the
                // file exists
                if( success )
                {
                    deleteParents(file);
                }
                
                // Make sure to commit our outstanding work every 100
                // iterations. Otherwise you risk losing the entire transaction
                // if we hit an exception, which isn't useful at all for large
                // amounts of bitstreams.
                commitCounter++;
                if (commitCounter % 100 == 0)
                {
                	System.out.print("Committing changes to the database...");
                    context.commit();
                    System.out.println(" Done!");
                }
            }

            context.complete();
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException sqle)
        {
            if (verbose)
            {
                System.err.println("Error: " + sqle.getMessage());
            }
            context.abort();
            throw sqle;
        }
        catch (IOException ioe)
        {
            if (verbose)
            {
                System.err.println("Error: " + ioe.getMessage());
            }
            context.abort();
            throw ioe;
        }
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return true if this file is too recent to be deleted, false otherwise.
     * 
     * @param file
     *            The file to check
     * @return True if this file is too recent to be deleted
     */
    private static boolean isRecent(File file)
    {
        long lastmod = file.lastModified();
        long now = new java.util.Date().getTime();

        if (lastmod >= now)
        {
            return true;
        }

        // Less than one hour old
        return (now - lastmod) < (1 * 60 * 1000);
    }

    /**
     * Delete empty parent directories.
     * 
     * @param file
     *            The file with parent directories to delete
     */
    private static synchronized void deleteParents(File file) {
        if (file == null) {
            return;
        }
        
        File tmp = file;
        
        for (int i = 0; i < directoryLevels; i++) {
            File directory = tmp.getParentFile();
            File[] files = directory.listFiles();

            // Only delete empty directories
            if (files.length != 0) {
                break;
            }

            directory.delete();
            tmp = directory;
        }
    }
    
    /**
     * Return the file corresponding to a bitstream. It's safe to pass in
     * <code>null</code>.
     * 
     * @param bitstream
     *            the database table row for the bitstream. Can be
     *            <code>null</code>
     * 
     * @return The corresponding file in the file system, or <code>null</code>
     * 
     * @exception IOException
     *                If a problem occurs while determining the file
     */
    private static File getFile(TableRow bitstream) throws IOException
    {
        // Check that bitstream is not null
        if (bitstream == null)
        {
            return null;
        }

        // Get the store to use
        int storeNumber = bitstream.getIntColumn("store_number");
        if(storeNumber == S3_ASSETSTORE) {
            throw new IOException("Bitstreams in Amazon S3 cannot be retrieved with getFile()");
        }

        // Default to zero ('assetstore.dir') for backwards compatibility
        if (storeNumber == -1) {
            storeNumber = 0;
        }

        File assetstore = assetStores[storeNumber];

        // turn the internal_id into a file path relative to the assetstore
        // directory
        String sInternalId = bitstream.getStringColumn("internal_id");

        // there are 2 cases:
        // -conventional bitstream, conventional storage
        // -registered bitstream, conventional storage
        // conventional bitstream - dspace ingested, dspace random name/path
        // registered bitstream - registered to dspace, any name/path
        String sIntermediatePath = null;
        if (isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(REGISTERED_FLAG.length());
            sIntermediatePath = "";
        } else {
            // Sanity Check: If the internal ID contains a
            // pathname separator, it's probably an attempt to
            // make a path traversal attack, so ignore the path
            // prefix.  The internal-ID is supposed to be just a
            // filename, so this will not affect normal operation.
            if (sInternalId.indexOf(File.separator) != -1) {
                sInternalId = sInternalId.substring(sInternalId.lastIndexOf(File.separator) + 1);
            }
			
            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        StringBuffer bufFilename = new StringBuffer();
        bufFilename.append(assetstore.getCanonicalPath());
        bufFilename.append(File.separator);
        bufFilename.append(sIntermediatePath);
        bufFilename.append(sInternalId);
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + sInternalId + " is "
                      + bufFilename.toString());
        }
        return new File(bufFilename.toString());
    }

	/**
	 * Return the intermediate path derived from the internal_id. This method
	 * splits the id into groups which become subdirectories.
	 *
	 * @param iInternalId
	 *            The internal_id
	 * @return The path based on the id without leading or trailing separators
	 */
	private static String getIntermediatePath(String iInternalId) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < directoryLevels; i++) {
			int digits = i * digitsPerLevel;
			if (i > 0) {
				buf.append(File.separator);
			}
			buf.append(iInternalId.substring(digits, digits
							+ digitsPerLevel));
		}
		buf.append(File.separator);
		return buf.toString();
	}

}
