/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * DSpace (or "Directory Scatter" if you prefer) asset store implemented for the Hadoop Filesystem API..
 * Implements a directory 'scatter' algorithm to avoid limits on
 * files per directory.
 *
 * A lot of code have been lifted directly from DSBitStoreService
 * @see DSBitStoreService
 * @author Asger Askov Blekinge
 */
public class HadoopBitStoreService implements BitStoreService
{


    /** log4j log */
    private static Logger log = Logger.getLogger(DSBitStoreService.class);

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

    // Checksum algorithm
    private static final String CSA = "MD5";

    /** the asset directory */
    private Path baseDir;

    /** The file system path for the hadoop filesystem. Connect to a hdfs cluster with a path like "hdfs://namenode:8020" and to a local file with a path like "file:///"
     */
    private String defaultFS;

    /**
     * The hadoop filesystem connection
     */
    private FileSystem fileSystem;


    @Override
    public void init() throws IOException
    {
    }


    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    public String getDefaultFS() {
        return defaultFS;
    }

    public void setDefaultFS(String defaultFS) {
        this.defaultFS = defaultFS;
    }

    /**
     * Utility method to initialise and/or get the filesystem. The first invocation initiatializes the file system connection, and the rest just return this. Synchronized so no initialise race conditions
     * @return the file system object
     * @throws IOException if the file system could not be initialised
     */
    protected synchronized FileSystem getFileSystem() throws IOException {
        if (fileSystem == null)
        {
            Configuration configuration = new Configuration();
            //defaultFS = "hdfs://namenode:8020";
            configuration.set(org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, defaultFS);
            fileSystem = FileSystem.get(configuration);
        }
        return fileSystem;

    }

    /**
     * Retrieve the bits for the asset with ID. If the asset does not
     * exist, returns null.
     *
     * @param bitstream
     *            The ID of the asset to retrieve
     * @throws java.io.IOException
     *                If a problem occurs while retrieving the bits
     *
     * @return The stream of bits, or null
     */
    @Override
    public InputStream get(Bitstream bitstream) throws IOException
    {
        try
        {
            FileSystem fs = getFileSystem();
            Path file = getFile(bitstream, baseDir);
            if (file != null && fs.exists(file))
            {
                return fs.open(file);
            }
            else
            {
                return null;
            }
        }
        catch (Exception e)
        {
            throw new IOException("get(" + bitstream.getInternalId() + ")", e);
        }
    }

    /**
     * Store a stream of bits.
     *
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param in
     *            The stream of bits to store
     * @throws java.io.IOException
     *             If a problem occurs while storing the bits
     */
    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException
    {
        try {
            FileSystem fs = getFileSystem();

            Path file = getFile(bitstream, baseDir);

            // Make the parent dirs if necessary
            Path parent = file.getParent();
            if (!fs.exists(parent)) {
                fs.mkdirs(parent);
            }

            bitstream.setChecksumAlgorithm(CSA);
            MessageDigest messageDigest = getMessageDigest(CSA);
            try ( //Try-with-resources to autoclose the streams
                    //Create the corresponding file and open it
                    OutputStream fos = fs.create(file);
                    // digest input stream that will work out the MD5 when read
                    DigestInputStream dis = new DigestInputStream(in, messageDigest);)
            {
                Utils.bufferedCopy(dis, fos);
            }
            byte[] digest = messageDigest.digest();
            bitstream.setChecksum(Utils.toHex(digest));
            bitstream.setSizeBytes(fs.getFileStatus(file).getLen());

        } catch (Exception e)
        {
            throw new IOException("put(" + bitstream.getInternalId() + ", inputstream)", e);
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param bitstream
     *            The asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     *
     * @throws java.io.IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     */
    @Override
    public Map about(Bitstream bitstream, Map attrs) throws IOException
    {
        try
        {
            FileSystem fs = getFileSystem();
            // potentially expensive, since it may calculate the checksum
            Path file = getFile(bitstream, baseDir);
            if (file != null && fs.exists(file))
            {
                FileStatus fileStatus = fs.getFileStatus(file);
                if (attrs.containsKey("size_bytes"))
                {
                    attrs.put("size_bytes", fileStatus.getLen());
                }
                if (attrs.containsKey("checksum"))
                {
                    MessageDigest messageDigest = getMessageDigest(CSA);
                    // generate checksum by reading the bytes
                    try (OutputStream devNull = new IOUtils.NullOutputStream();
                         DigestInputStream dis = new DigestInputStream(fs.open(file), messageDigest) )
                    {
                        Utils.bufferedCopy(dis, devNull);

                    }
                    attrs.put("checksum", Utils.toHex(messageDigest.digest()));
                    attrs.put("checksum_algorithm", CSA);
                }
                if (attrs.containsKey("modified"))
                {
                    attrs.put("modified", String.valueOf(fileStatus.getModificationTime()));
                }
                return attrs;
            }
            return null;
        } catch (Exception e)
        {
            throw new IOException("about(" + bitstream.getInternalId() + ")", e);
        }
    }

    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param bitstream
     *            The asset to delete
     * @throws java.io.IOException
     *             If a problem occurs while removing the asset
     */

    @Override
    public void remove(Bitstream bitstream) throws IOException
    {
        try
        {
            FileSystem fs = getFileSystem();
            Path file = getFile(bitstream, baseDir);
            if (file != null && fs.exists(file)) // only if file exists
            {
                if (fs.delete(file,false)) //true if delete succeeds
                {
                    deleteParents(file, fs);
                }
            }
            else // File does not exist
            {
                log.warn("Attempt to remove non-existent asset. ID: " + bitstream.getInternalId());
            }
        } catch (Exception e)
        {
            throw new IOException("remove(" + bitstream.getInternalId() + ")", e);
        }
    }



    /**
     * Return an identifier unique to this asset store instance
     *
     * @return a unique ID
     */
    @Override
    public String generateId()
    {
        return Utils.generateKey();
    }


    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Delete empty parent directories.
     *
     * @param file
     *            The file with parent directories to delete
     * @param fs the filesystem to use
     */
    private synchronized void deleteParents(Path file, FileSystem fs) throws IOException
    {
        if (file == null)
        {
            return;
        }

        Path tmp = file;
        for (int i = 0; i < directoryLevels; i++)
        {
            Path directory = tmp.getParent();
            try
            {
                //recursive=false, this throws IOException when attempting to delete non-empty dir.
                fs.delete(directory, false);
            } catch (IOException e)
            {
                //Directory not empty...
                return;
            }
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
     * @throws IOException
     *                If a problem occurs while determining the file
     */
    protected Path getFile(Bitstream bitstream, Path baseDir) throws IOException
    {
        // Check that bitstream is not null
        if (bitstream == null)
        {
            return null;
        }

        // turn the internal_id into a file path relative to the assetstore
        // directory
        String sInternalId = bitstream.getInternalId();

        // there are 4 cases:
        // -conventional bitstream, conventional storage
        // -conventional bitstream, srb storage
        // -registered bitstream, conventional storage
        // -registered bitstream, srb storage
        // conventional bitstream - dspace ingested, dspace random name/path
        // registered bitstream - registered to dspace, any name/path
        String sIntermediatePath = null;
        if (isRegisteredBitstream(sInternalId))
        {
            sInternalId = sInternalId.substring(REGISTERED_FLAG.length());
            sIntermediatePath = "";
        }
        else
            {

            // Sanity Check: If the internal ID contains a
            // pathname separator, it's probably an attempt to
            // make a path traversal attack, so ignore the path
            // prefix.  The internal-ID is supposed to be just a
            // filename, so this will not affect normal operation.
            if (sInternalId.contains(File.separator))
            {
                sInternalId = sInternalId.substring(sInternalId.lastIndexOf(File.separator) + 1);
            }

            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(sIntermediatePath);
        bufFilename.append(sInternalId);
        if (log.isDebugEnabled())
        {
            log.debug("Local filename for " + sInternalId + " is "
                      + bufFilename.toString());
        }
        return new Path(baseDir,bufFilename.toString());
    }

    /**
     * Return the intermediate path derived from the internal_id. This method
     * splits the id into groups which become subdirectories.
     *
     * @param iInternalId
     *            The internal_id
     * @return The path based on the id without leading or trailing separators
     */
    protected String getIntermediatePath(String iInternalId)
    {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < directoryLevels; i++)
        {
            int digits = i * digitsPerLevel;
            if (i > 0)
            {
                buf.append(File.separator);
            }
            buf.append(iInternalId.substring(digits, digits
                                                     + digitsPerLevel));
        }
        buf.append(File.separator);
        return buf.toString();
    }


    protected MessageDigest getMessageDigest(String CSA) {
        MessageDigest CSA_instance;
        try {
            CSA_instance = MessageDigest.getInstance(CSA);
        } catch (NoSuchAlgorithmException nsae) { // Should never happen
            throw new RuntimeException("Caught NoSuchAlgorithmException", nsae);
        }
        return CSA_instance;
    }


    protected static final String REGISTERED_FLAG = "-R";
    public static boolean isRegisteredBitstream(String internalId)
    {
        return internalId.startsWith(REGISTERED_FLAG);
    }

}
