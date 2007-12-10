/*
 * BitstreamStorageManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.content.Bitstream;
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileOutputStream;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;

/**
 * <P>
 * Stores, retrieves and deletes bitstreams.
 * </P>
 * 
 * <P>
 * Presently, asset stores are specified in <code>dspace.cfg</code>. Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property. The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and DSpace (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.
 * </P>
 * 
 * <P>
 * Mods by David Little, UCSD Libraries 12/21/04 to allow the registration of
 * files (bitstreams) into DSpace.
 * </P>
 * 
 * <p>Cleanup integration with checker package by Nate Sarr 2006-01. N.B. The 
 * dependency on the checker package isn't ideal - a Listener pattern would be 
 * better but was considered overkill for the purposes of integrating the checker.
 * It would be worth re-considering a Listener pattern if another package needs to 
 * be notified of BitstreamStorageManager actions.</p> 
 *
 * @author Peter Breton, Robert Tansley, David Little, Nathan Sarr
 * @version $Revision$
 */
public class BitstreamStorageManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);

	/**
	 * The asset store locations. The information for each GeneralFile in the
	 * array comes from dspace.cfg, so see the comments in that file.
	 *
	 * If an array element refers to a conventional (non_SRB) asset store, the
	 * element will be a LocalFile object (similar to a java.io.File object)
	 * referencing a local directory under which the bitstreams are stored.
	 *
	 * If an array element refers to an SRB asset store, the element will be an
	 * SRBFile object referencing an SRB 'collection' (directory) under which
	 * the bitstreams are stored.
	 *
	 * An SRBFile object is obtained by (1) using dspace.cfg properties to
	 * create an SRBAccount object (2) using the account to create an
	 * SRBFileSystem object (similar to a connection) (3) using the
	 * SRBFileSystem object to create an SRBFile object
	 */
	private static GeneralFile[] assetStores;

    /** The asset store to use for new bitstreams */
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
	public static final String REGISTERED_FLAG = "-R";

    /* Read in the asset stores from the config. */
    static
    {
        ArrayList stores = new ArrayList();

		// 'assetstore.dir' is always store number 0
		String sAssetstoreDir = ConfigurationManager
				.getProperty("assetstore.dir");
 
		// see if conventional assetstore or srb
		if (sAssetstoreDir != null) {
			stores.add(sAssetstoreDir); // conventional (non-srb)
		} else if (ConfigurationManager.getProperty("srb.host") != null) {
			stores.add(new SRBAccount( // srb
					ConfigurationManager.getProperty("srb.host"),
					ConfigurationManager.getIntProperty("srb.port"),
					ConfigurationManager.getProperty("srb.username"),
					ConfigurationManager.getProperty("srb.password"),
					ConfigurationManager.getProperty("srb.homedirectory"),
					ConfigurationManager.getProperty("srb.mdasdomainname"),
					ConfigurationManager
							.getProperty("srb.defaultstorageresource"),
					ConfigurationManager.getProperty("srb.mcatzone")));
		} else {
			log.error("No default assetstore");
		}

		// read in assetstores .1, .2, ....
		for (int i = 1;; i++) { // i == 0 is default above
			sAssetstoreDir = ConfigurationManager.getProperty("assetstore.dir."
					+ i);

			// see if 'i' conventional assetstore or srb
			if (sAssetstoreDir != null) { 		// conventional (non-srb)
				stores.add(sAssetstoreDir);
			} else if (ConfigurationManager.getProperty("srb.host." + i)
					!= null) { // srb
				stores.add(new SRBAccount(
						ConfigurationManager.getProperty("srb.host." + i),
						ConfigurationManager.getIntProperty("srb.port." + i),
						ConfigurationManager.getProperty("srb.username." + i),
						ConfigurationManager.getProperty("srb.password." + i),
						ConfigurationManager
								.getProperty("srb.homedirectory." + i),
						ConfigurationManager
								.getProperty("srb.mdasdomainname." + i),
						ConfigurationManager
								.getProperty("srb.defaultstorageresource." + i),
						ConfigurationManager.getProperty("srb.mcatzone." + i)));
			} else {
				break; // must be at the end of the assetstores
			}
		}

		// convert list to array
		// the elements (objects) in the list are class
		//   (1) String - conventional non-srb assetstore
		//   (2) SRBAccount - srb assetstore
		assetStores = new GeneralFile[stores.size()];
		for (int i = 0; i < stores.size(); i++) {
			Object o = stores.get(i);
			if (o == null) { // I don't know if this can occur
				log.error("Problem with assetstore " + i);
			}
			if (o instanceof String) {
				assetStores[i] = new LocalFile((String) o);
			} else if (o instanceof SRBAccount) {
				SRBFileSystem srbFileSystem = null;
				try {
					srbFileSystem = new SRBFileSystem((SRBAccount) o);
				} catch (NullPointerException e) {
					log.error("No SRBAccount for assetstore " + i);
				} catch (IOException e) {
					log.error("Problem getting SRBFileSystem for assetstore"
							+ i);
				}
				if (srbFileSystem == null) {
					log.error("SRB FileSystem is null for assetstore " + i);
				}
				String sSRBAssetstore = null;
				if (i == 0) { // the zero (default) assetstore has no suffix
					sSRBAssetstore = ConfigurationManager
							.getProperty("srb.parentdir");
				} else {
					sSRBAssetstore = ConfigurationManager
							.getProperty("srb.parentdir." + i);
				}
				if (sSRBAssetstore == null) {
					log.error("srb.parentdir is undefined for assetstore " + i);
				}
				assetStores[i] = new SRBFile(srbFileSystem, sSRBAssetstore);
			} else {
				log.error("Unexpected " + o.getClass().toString()
						+ " with assetstore " + i);
			}
		}

        // Read asset store to put new files in. Default is 0.
        incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
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
     * 
     * @return The ID of the stored bitstream
     */
    public static void store(Context context, Bitstream bitstream,
            InputStream is)
        throws AuthorizeException, IOException
    {
        BitstreamDAO dao = BitstreamDAOFactory.getInstance(context);

        // Create internal ID
        String id = Utils.generateKey();

        // Create a deleted bitstream row, using a separate DB connection
        Context tempContext = null;

        // Put something into the storage layer to attach the file to.
        storeInitialMetadata(context, dao, bitstream, incoming, id);

        // Where on the file system will this new bitstream go?
		GeneralFile file = getFile(incoming, id);

        // Make the parent dirs if necessary
		GeneralFile parent = file.getParentFile();

        if (!parent.exists())
        {
            parent.mkdirs();
        }

        //Create the corresponding file and open it
        file.createNewFile();

		GeneralFileOutputStream fos = FileFactory.newFileOutputStream(file);

		// Read through a digest input stream that will work out the MD5
        DigestInputStream dis = null;

        try
        {
            dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        }
        // Should never happen
        catch (NoSuchAlgorithmException nsae)
        {
            log.warn("Caught NoSuchAlgorithmException", nsae);
        }

        Utils.bufferedCopy(dis, fos);
        fos.close();
        is.close();

        bitstream.setSize(file.length());

        bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
        bitstream.setChecksumAlgorithm("MD5");
        bitstream.setDeleted(false);

        dao.update(bitstream);

        if (log.isDebugEnabled())
        {
            log.debug("Stored bitstream " + bitstream.getID() + " in file "
                    + file.getAbsolutePath());
        }
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
	 * @throws IOExeption
	 */
    public static void register(Context context, Bitstream bitstream, int
            assetstore, String bitstreamPath)
        throws AuthorizeException, IOException
    {
        BitstreamDAO dao = BitstreamDAOFactory.getInstance(context);

		// mark this bitstream as a registered bitstream
		String sInternalId = REGISTERED_FLAG + bitstreamPath;

        // Put something into the storage layer to attach the file to.
        storeInitialMetadata(context, dao, bitstream, assetstore, sInternalId);

		// get a reference to the file
		GeneralFile file = getFile(assetstore, sInternalId);

		// read through a DigestInputStream that will work out the MD5
		//
		// DSpace refers to checksum, writes it in METS, and uses it as an
		// AIP filename (!), but never seems to validate with it. Furthermore,
		// DSpace appears to hardcode the algorithm to MD5 in some places--see 
		// METSExport.java.
		//
		// To remain compatible with DSpace we calculate an MD5 checksum on 
		// LOCAL registered files. But for REMOTE (e.g. SRB) files we 
		// calculate an MD5 on just the fileNAME. The reasoning is that in the 
		// case of a remote file, calculating an MD5 on the file itself will
		// generate network traffic to read the file's bytes. In this case it 
		// would be better have a proxy process calculate MD5 and store it as 
		// an SRB metadata attribute so it can be retrieved simply from SRB.
		//
		// TODO set this up as a proxy server process so no net activity
		
		// FIXME this is a first class HACK! for the reasons described above
		if (file instanceof LocalFile) 
		{

			// get MD5 on the file for local file
			DigestInputStream dis = null;
			try 
			{
				dis = new DigestInputStream(FileFactory.newFileInputStream(file), 
						MessageDigest.getInstance("MD5"));
			} 
			catch (NoSuchAlgorithmException e) 
			{
				log.warn("Caught NoSuchAlgorithmException", e);
				throw new IOException("Invalid checksum algorithm");
			}
			catch (IOException e) 
			{
				log.error("File: " + file.getAbsolutePath() 
						+ " to be registered cannot be opened - is it "
						+ "really there?");
				throw e;
			}
			final int BUFFER_SIZE = 1024 * 4;
			final byte[] buffer = new byte[BUFFER_SIZE];
			while (true) 
			{
				final int count = dis.read(buffer, 0, BUFFER_SIZE);
				if (count == -1) 
				{
					break;
				}
			}
            bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
			dis.close();
		} 
		else if (file instanceof SRBFile)
		{
			if (!file.exists())
			{
				log.error("File: " + file.getAbsolutePath() 
						+ " is not in SRB MCAT");
				throw new IOException("File is not in SRB MCAT");
			}

			// get MD5 on just the filename (!) for SRB file
			int iLastSlash = bitstreamPath.lastIndexOf('/');
			String sFilename = bitstreamPath.substring(iLastSlash + 1);
			MessageDigest md = null;
			try 
			{
				md = MessageDigest.getInstance("MD5");
			} 
			catch (NoSuchAlgorithmException e) 
			{
				log.error("Caught NoSuchAlgorithmException", e);
				throw new IOException("Invalid checksum algorithm");
			}
            bitstream.setChecksum(
                    Utils.toHex(md.digest(sFilename.getBytes())));
		}
		else
		{
			throw new IOException("Unrecognized file type - "
					+ "not local, not SRB");
		}

		bitstream.setChecksumAlgorithm("MD5");
		bitstream.setSize(file.length());
		bitstream.setDeleted(false);

        dao.update(bitstream);

		if (log.isDebugEnabled()) 
		{
			log.debug("Stored bitstream " + bitstream.getID() + " in file "
					+ file.getAbsolutePath());
		}
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
     * 
     * @return The stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id)
            throws IOException
    {
        Bitstream b = BitstreamDAOFactory.getInstance(context).retrieve(id);
        GeneralFile file = null;

        if (b != null)
        {
            file = getFile(b.getStoreNumber(), b.getInternalID());
        }

		return (file != null) ? FileFactory.newFileInputStream(file) : null;
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
     */
    public static void cleanup(boolean deleteDbRecords)
        throws IOException, AuthorizeException
    {
        Context context = null;
        BitstreamInfoDAO bitstreamInfoDAO = new BitstreamInfoDAO();
        int commit_counter = 0;

        try
        {
            context = new Context();
            BitstreamDAO bitstreamDAO =
                BitstreamDAOFactory.getInstance(context);

            for (Bitstream bitstream : bitstreamDAO.getDeletedBitstreams())
            {
                int bid = bitstream.getID();

                GeneralFile file = getFile(bitstream.getStoreNumber(),
                        bitstream.getInternalID());

                // Make sure entries which do not exist are removed
                if (file == null || !file.exists())
                {
                    log.debug("file is null");
                    if (deleteDbRecords)
                    {
                        log.debug("deleting record");
                        bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);

                        // This is typically run from the command line where
                        // no user is authenticated, so we just grant permission
                        boolean ignoreAuth = context.ignoreAuthorization();
                        context.setIgnoreAuthorization(true);
                        bitstreamDAO.remove(bid);
                        context.setIgnoreAuthorization(ignoreAuth);
                    }
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                if (isRecent(file))
                {
                	log.debug("file is recent");
                    continue;
                }

                if (deleteDbRecords)
                {
                    log.debug("deleting db record");
                    bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);

                    // This is typically run from the command line where
                    // no user is authenticated, so we just grant permission
                    boolean ignoreAuth = context.ignoreAuthorization();
                    context.setIgnoreAuthorization(true);
                    bitstreamDAO.remove(bid);
                    context.setIgnoreAuthorization(ignoreAuth);
                }

				if (isRegisteredBitstream(bitstream.getInternalID()))
                {
				    continue;			// do not delete registered bitstreams
				}

                boolean success = file.delete();

                if (log.isDebugEnabled())
                {
                    log.debug("Deleted bitstream " + bid + " (file "
                            + file.getAbsolutePath() + ") with result "
                            + success);
                }

                // if the file was deleted then
                // try deleting the parents
                // Otherwise the cleanup script is set to 
                // leave the db records then the file
                // and directories have already been deleted
                // if this is turned off then it still looks like the
                // file exists
                if (success)
                {
                    deleteParents(file);
                }
                
                // Make sure to commit our outstanding work every 100
                // iterations. Otherwise you risk losing the entire transaction
                // if we hit an exception, which isn't useful at all for large
                // amounts of bitstreams.
                commit_counter++;
                if (commit_counter % 100 == 0)
                {
                	context.commit();
                }
            }

            context.complete();
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException sqle)
        {
            context.abort();

            throw new RuntimeException(sqle);
        }
        catch (IOException ioe)
        {
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
    private static boolean isRecent(GeneralFile file)
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
    private synchronized static void deleteParents(GeneralFile file)
    {
        if (file == null )
        {
            return;
        }
 
		GeneralFile tmp = file;

        for (int i = 0; i < directoryLevels; i++)
        {

			GeneralFile directory = tmp.getParentFile();
			GeneralFile[] files = directory.listFiles();

            // Only delete empty directories
            if (files.length != 0)
            {
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
     * @param storeNumber the store number that the bitstream is in
     * @param sInternalID the internal id of the bitstream
     * 
     * @return The corresponding file in the file system, or <code>null</code>
     * 
     * @exception IOException
     *                If a problem occurs while determining the file
     */
    public static GeneralFile getFile(int storeNumber, String sInternalId)
        throws IOException
    {
        // Default to zero ('assetstore.dir') for backwards compatibility
        if (storeNumber == -1)
        {
            storeNumber = 0;
        }

		GeneralFile assetstore = assetStores[storeNumber];

		// turn the internal_id into a file path relative to the assetstore
		// directory
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
			if (sInternalId.indexOf(File.separator) != -1)
            {
				sInternalId = sInternalId.substring(
                        sInternalId.lastIndexOf(File.separator)+1);
            }
			
			sIntermediatePath = getIntermediatePath(sInternalId);
		}

		StringBuffer bufFilename = new StringBuffer();
		if (assetstore instanceof LocalFile)
        {
			bufFilename.append(assetstore.getCanonicalPath());
			bufFilename.append(File.separator);
			bufFilename.append(sIntermediatePath);
			bufFilename.append(sInternalId);
			if (log.isDebugEnabled())
            {
				log.debug("Local filename for " + sInternalId + " is "
						+ bufFilename.toString());
			}
			return new LocalFile(bufFilename.toString());
		}
		if (assetstore instanceof SRBFile)
        {
			bufFilename.append(sIntermediatePath);
			bufFilename.append(sInternalId);

			if (log.isDebugEnabled())
            {
				log.debug("SRB filename for " + sInternalId + " is "
						+ ((SRBFile) assetstore).toString()
						+ bufFilename.toString());
			}
			return new SRBFile((SRBFile) assetstore, bufFilename.toString());
		}
		return null;
    }

	/**
	 * Return the intermediate path derived from the internal_id. This method
	 * splits the id into groups which become subdirectories.
	 *
	 * @param iInternalId
	 *            The internal_id
	 * @return The path based on the id without leading or trailing separators
	 */
	private static String getIntermediatePath(String iInternalId)
    {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < directoryLevels; i++)
        {
			int digits = i * digitsPerLevel;
			if (i > 0)
            {
				buf.append(File.separator);
			}
            buf.append(iInternalId.substring(digits, digits + digitsPerLevel));
		}
		buf.append(File.separator);
		return buf.toString();
	}

    /**
     * This inserts some metadata into the storage layer using a separate
     * Context to ensure that there is some record of the file in the metadata
     * store.
     */
    private static void storeInitialMetadata(Context context,
            BitstreamDAO dao, Bitstream bitstream,
            int assetstore, String internalID)
        throws AuthorizeException
    {
        Context tempContext = null;

        // Make sure there's something in the database from the other
        // Context, then run an update into that from the temporary one
        try
        {
            context.commit();
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }

		try
        {
            /*
             * Set the store number of the new bitstream If you want to use some
             * other method of working out where to put a new bitstream, here's
             * where it should go (only applicable for store(), not register()).
             */
			bitstream.setStoreNumber(assetstore);
			bitstream.setInternalID(internalID);
			bitstream.setDeleted(true);

			tempContext = new Context();
            tempContext.setCurrentUser(context.getCurrentUser());

            BitstreamDAO tempDAO = BitstreamDAOFactory.getInstance(tempContext);
            tempDAO.update(bitstream);
			tempContext.complete();
		}
        catch (SQLException sqle)
        {
			if (tempContext != null)
            {
				tempContext.abort();
			}

			throw new RuntimeException(sqle);
		}
    }
}
