/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitStore;

/**
 * Native DSpace (or "Directory Scatter" if you prefer) asset store.
 * Implements a directory 'scatter' algorithm to avoid OS limits on
 * files per directory.
 * 
 * @author Peter Breton, Robert Tansley, Richard Rodgers, Peter Dietz
 */

public class DSBitStore implements BitStore
{
    /** log4j log */
    private static Logger log = Logger.getLogger(DSBitStore.class);
    
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
	private File baseDir = null;
	
	public DSBitStore()
	{
	}
	
	/**
     * Initialize the asset store
     * 
     * @param config
     *        String used to characterize configuration - the name
     *        of the directory root of the asset store
     */
	public void init(String config)
	{
		// the config string contains just the asset store directory path
		baseDir = new File(config);
	}
	
	/**
     * Return an identifier unique to this asset store instance
     * 
     * @return a unique ID
     */
	public String generateId()
	{
        return Utils.generateKey();
	}

	/**
     * Retrieve the bits for the asset with ID. If the asset does not
     * exist, returns null.
     * 
     * @param id
     *            The ID of the asset to retrieve
     * @exception IOException
     *                If a problem occurs while retrieving the bits
     * 
     * @return The stream of bits, or null
     */
	public InputStream get(String id) throws IOException
	{
		return new FileInputStream(getFile(id));
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
     * @exception IOException
     *             If a problem occurs while storing the bits
     * 
     * @return Map containing technical metadata (size, checksum, etc)
     */
	public Map put(InputStream in, String id) throws IOException
	{
		File file = getFile(id);
		
		// Make the parent dirs if necessary
		File parent = file.getParentFile();
        if (!parent.exists())
        {
            parent.mkdirs();
        }
        //Create the corresponding file and open it
        file.createNewFile();
        
		FileOutputStream fos = new FileOutputStream(file);

		// Read through a digest input stream that will work out the MD5
        DigestInputStream dis = null;

        try
        {
            dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));
        }
        // Should never happen
        catch (NoSuchAlgorithmException nsae)
        {
            log.warn("Caught NoSuchAlgorithmException", nsae);
        }

        Utils.bufferedCopy(dis, fos);
        fos.close();
        in.close();
     
        Map attrs = new HashMap();
        attrs.put(Bitstream.SIZE_BYTES, file.length());
        attrs.put(Bitstream.CHECKSUM, Utils.toHex(dis.getMessageDigest().digest()));
        attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
        return attrs;
	}
	
    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param id
     *            The ID of the asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     * 
     * @exception IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     */
	public Map about(String id, Map attrs) throws IOException
	{
		// potentially expensive, since it may calculate the checksum
		File file = getFile(id);
		if (file != null && file.exists())
		{
		    if (attrs.containsKey(Bitstream.SIZE_BYTES))
		    {
		        attrs.put(Bitstream.SIZE_BYTES, file.length());
		    }
		    if (attrs.containsKey(Bitstream.CHECKSUM))
		    {
		        // generate checksum by reading the bytes
			    DigestInputStream dis = null;
			    try 
			    {
				    FileInputStream fis = new FileInputStream(file);
				    dis = new DigestInputStream(fis, MessageDigest.getInstance(CSA));
			    } 
			    catch (NoSuchAlgorithmException e) 
			    {
				    log.warn("Caught NoSuchAlgorithmException", e);
				    throw new IOException("Invalid checksum algorithm");
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
			    attrs.put(Bitstream.CHECKSUM, Utils.toHex(dis.getMessageDigest().digest()));
			    attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
			    dis.close();
		    }
			if (attrs.containsKey("modified"))
			{
			    attrs.put("modified", String.valueOf(file.lastModified()));
			}			
			return attrs;
		}
		return null;
	}
	
    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param id
     *            The ID of the asset to delete
     * @exception IOException
     *             If a problem occurs while removing the asset
     */
	public void remove(String id) throws IOException
	{
		File file = getFile(id);
		if (file != null)
		{
		    if (file.delete())
		    {
			    deleteParents(file);
		    }
		}
        else
        {
        	log.warn("Attempt to remove non-existent asset. ID: " + id);
        }
	}
	
    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////
	
	/**
     * Delete empty parent directories.
     * 
     * @param file
     *            The file with parent directories to delete
     */
    private synchronized static void deleteParents(File file)
    {
        if (file == null)
        {
            return;
        }
 
		File tmp = file;

        for (int i = 0; i < directoryLevels; i++)
        {
			File directory = tmp.getParentFile();
			File[] files = directory.listFiles();

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
	 * Return the File for the passed internal_id.
	 *
	 * @param id
	 *            The internal_id
	 * @return The file resolved from the id
	 */
	private File getFile(String id) throws IOException
	{
		StringBuffer sb = new StringBuffer();
		sb.append(baseDir.getCanonicalPath());
		sb.append(File.separator);
		sb.append(getIntermediatePath(id));
		sb.append(id);
		if (log.isDebugEnabled())
		{
			log.debug("Local filename for " + id + " is " + sb.toString());
		}
		return new File(sb.toString());
	}
	
	/**
	 * Return the path derived from the internal_id. This method
	 * splits the id into groups which become subdirectories.
	 *
	 * @param id
	 *            The internal_id
	 * @return The path based on the id without leading or trailing separators
	 */
	private static String getIntermediatePath(String id)
	{
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < directoryLevels; i++) {
			int digits = i * digitsPerLevel;
			if (i > 0)
			{
				buf.append(File.separator);
			}
			buf.append(id.substring(digits, digits	+ digitsPerLevel));
		}
		buf.append(File.separator);
		return buf.toString();
	}
}
