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
 * Created by abr on 23-02-17.
 */
public class HadoopBitStoreService implements BitStoreService
{


    /** log4j log */
    private static Logger log = Logger.getLogger(DSBitStoreService.class);
    private FileSystem fs;
    private Configuration configuration;

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
    private String defaultFS;


    @Override
    public void init() throws IOException
    {
        configuration = new Configuration();
        //defaultFS = "hdfs://namenode:8020";
        configuration.set(org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, defaultFS);
        fs = FileSystem.get(configuration);
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
            return fs.open(getFile(bitstream));
        } catch (Exception e)
        {
            log.error("get(" + bitstream.getInternalId() + ")", e);
            throw new IOException(e);
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
        try
        {
            Path file = getFile(bitstream);

            // Make the parent dirs if necessary
            Path parent = file.getParent();
            if (!fs.exists(parent)) {
                fs.mkdirs(parent);
            }

            byte[] digest;
            try (//Create the corresponding file and open it
                 OutputStream fos = fs.create(file);
                 // Read through a digest input stream that will work out the MD5
                 DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));)
            {

                Utils.bufferedCopy(dis, fos);

                digest = dis.getMessageDigest().digest();
            } catch (NoSuchAlgorithmException nsae)
            { // Should never happen
                throw new RuntimeException("Caught NoSuchAlgorithmException", nsae);
            }


            bitstream.setSizeBytes(fs.getFileStatus(file).getLen());
            bitstream.setChecksum(Utils.toHex(digest));
            bitstream.setChecksumAlgorithm(CSA);
        } catch (Exception e)
        {
            log.error("put(" + bitstream.getInternalId() + ", inputstream)", e);
            throw new IOException(e);
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
            // potentially expensive, since it may calculate the checksum
            Path file = getFile(bitstream);
            if (file != null && fs.exists(file))
            {
                FileStatus fileStatus = fs.getFileStatus(file);
                if (attrs.containsKey("size_bytes"))
                {
                    attrs.put("size_bytes", fileStatus.getLen());
                }
                if (attrs.containsKey("checksum"))
                {

                    // generate checksum by reading the bytes

                    MessageDigest digest;
                    try {
                        digest = MessageDigest.getInstance(CSA);
                    } catch (NoSuchAlgorithmException e)
                    {
                        log.warn("Caught NoSuchAlgorithmException", e);
                        throw new IOException("Invalid checksum algorithm");
                    }
                    try (DigestInputStream dis = new DigestInputStream(fs.open(file), digest);
                         OutputStream devNull = new IOUtils.NullOutputStream();)
                    {
                        Utils.bufferedCopy(dis, devNull);
                        attrs.put("checksum", Utils.toHex(dis.getMessageDigest().digest()));
                        attrs.put("checksum_algorithm", CSA);
                    }
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
            log.error("about(" + bitstream.getInternalId() + ")", e);
            throw new IOException(e);
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
            Path file = getFile(bitstream);
            if (file != null && fs.exists(file))
            {
                if (fs.delete(file,false))
                {
                    deleteParents(file);
                }
            }
            else
                {
                log.warn("Attempt to remove non-existent asset. ID: " + bitstream.getInternalId());
            }
        } catch (Exception e)
        {
            log.error("remove(" + bitstream.getInternalId() + ")", e);
            throw new IOException(e);
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
     */
    private synchronized void deleteParents(Path file) throws IOException
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
                fs.delete(directory,false);
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
    protected Path getFile(Bitstream bitstream) throws IOException
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

    protected final String REGISTERED_FLAG = "-R";
    public boolean isRegisteredBitstream(String internalId)
    {
        return internalId.startsWith(REGISTERED_FLAG);
    }

    public Path getBaseDir()
    {
        return baseDir;
    }

    public void setBaseDir(Path baseDir)
    {
        this.baseDir = baseDir;
    }

    public String getDefaultFS()
    {
        return defaultFS;
    }

    public void setDefaultFS(String defaultFS)
    {
        this.defaultFS = defaultFS;
    }
}
