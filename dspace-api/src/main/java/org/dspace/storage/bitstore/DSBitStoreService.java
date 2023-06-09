/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;

/**
 * Native DSpace (or "Directory Scatter" if you prefer) asset store.
 * Implements a directory 'scatter' algorithm to avoid OS limits on
 * files per directory.
 *
 * @author Peter Breton, Robert Tansley, Richard Rodgers, Peter Dietz
 */

public class DSBitStoreService extends BaseBitStoreService {
    /**
     * log4j log
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DSBitStoreService.class);

    /**
     * the asset directory
     */
    private File baseDir;
    protected final String REGISTERED_FLAG = "-R";

    public DSBitStoreService() {
    }

    /**
     * Initialize the asset store
     */
    public void init() {
        // the config string contains just the asset store directory path
        //set baseDir?
        this.initialized = true;
    }

    /**
     * Return an identifier unique to this asset store instance
     *
     * @return a unique ID
     */
    public String generateId() {
        return Utils.generateKey();
    }

    /**
     * Retrieve the bits for the asset with ID. If the asset does not
     * exist, returns null.
     *
     * @param bitstream The ID of the asset to retrieve
     * @return The stream of bits, or null
     * @throws java.io.IOException If a problem occurs while retrieving the bits
     */
    public InputStream get(Bitstream bitstream) throws IOException {
        try {
            return new FileInputStream(getFile(bitstream));
        } catch (Exception e) {
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
     * @param in The stream of bits to store
     * @throws java.io.IOException If a problem occurs while storing the bits
     */
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        try {
            File file = getFile(bitstream);

            // Make the parent dirs if necessary
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            //Create the corresponding file and open it
            file.createNewFile();

            try (
                    FileOutputStream fos = new FileOutputStream(file);
                    // Read through a digest input stream that will work out the MD5
                    DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));
            ) {
                Utils.bufferedCopy(dis, fos);
                in.close();

                bitstream.setSizeBytes(file.length());
                bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
                bitstream.setChecksumAlgorithm(CSA);
            } catch (NoSuchAlgorithmException nsae) {
                // Should never happen
                log.warn("Caught NoSuchAlgorithmException", nsae);
            }
        } catch (Exception e) {
            log.error("put(" + bitstream.getInternalId() + ", inputstream)", e);
            throw new IOException(e);
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param bitstream The asset to describe
     * @param attrs     A Map whose keys consist of desired metadata fields
     * @return attrs
     * A Map with key/value pairs of desired metadata
     * @throws java.io.IOException If a problem occurs while obtaining metadata
     */
    public Map about(Bitstream bitstream, Map attrs) throws IOException {
        try {
            // potentially expensive, since it may calculate the checksum
            File file = getFile(bitstream);
            if (file != null && file.exists()) {
                return super.about(file, attrs);
            }
            return null;
        } catch (Exception e) {
            log.error("about(" + bitstream.getInternalId() + ")", e);
            throw new IOException(e);
        }
    }

    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param bitstream The asset to delete
     * @throws java.io.IOException If a problem occurs while removing the asset
     */
    public void remove(Bitstream bitstream) throws IOException {
        try {
            File file = getFile(bitstream);
            if (file != null) {
                if (file.delete()) {
                    deleteParents(file);
                }
            } else {
                log.warn("Attempt to remove non-existent asset. ID: " + bitstream.getInternalId());
            }
        } catch (Exception e) {
            log.error("remove(" + bitstream.getInternalId() + ")", e);
            throw new IOException(e);
        }
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Delete empty parent directories.
     *
     * @param file The file with parent directories to delete
     */
    private synchronized static void deleteParents(File file) {
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
     * @param bitstream the database table row for the bitstream. Can be
     *                  <code>null</code>
     * @return The corresponding file in the file system, or <code>null</code>
     * @throws IOException If a problem occurs while determining the file
     */
    protected File getFile(Bitstream bitstream) throws IOException {
        // Check that bitstream is not null
        if (bitstream == null) {
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
        if (isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(REGISTERED_FLAG.length());
            sIntermediatePath = "";
        } else {

            // Sanity Check: If the internal ID contains a
            // pathname separator, it's probably an attempt to
            // make a path traversal attack, so ignore the path
            // prefix.  The internal-ID is supposed to be just a
            // filename, so this will not affect normal operation.
            sInternalId = this.sanitizeIdentifier(sInternalId);
            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(baseDir.getCanonicalFile());
        bufFilename.append(File.separator);
        bufFilename.append(sIntermediatePath);
        bufFilename.append(sInternalId);
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + sInternalId + " is "
                          + bufFilename.toString());
        }
        return new File(bufFilename.toString());
    }

    public boolean isRegisteredBitstream(String internalId) {
        return internalId.startsWith(REGISTERED_FLAG);
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }
}
