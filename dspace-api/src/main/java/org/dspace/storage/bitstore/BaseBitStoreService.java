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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Utils;

/**
 * BaseBitStoreService base implementation to store
 * and organize assets in digits.
 *
 */
public abstract class BaseBitStoreService implements BitStoreService {

    protected static Logger log = LogManager.getLogger(DSBitStoreService.class);
    // Checksum algorithm
    protected static final String CSA = "MD5";
    protected static final String MODIFIED = "modified";
    protected static final String CHECKSUM_ALGORITHM = "checksum_algorithm";
    protected static final String CHECKSUM = "checksum";
    protected static final String SIZE_BYTES = "size_bytes";

    protected boolean initialized = false;

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
    protected static final int digitsPerLevel = 2;
    protected static final int directoryLevels = 3;

    /**
     * Return the intermediate path derived from the internal_id. This method splits
     * the id into groups which become subdirectories.
     *
     * @param internalId The internal_id
     * @return The path based on the id without leading or trailing separators
     */
    protected String getIntermediatePath(String internalId) {
        StringBuilder path = new StringBuilder();
        if (StringUtils.isEmpty(internalId) || internalId.length() <= digitsPerLevel) {
            return path.append(internalId).append(File.separator).toString();
        }
        populatePathSplittingId(internalId, path);
        appendSeparator(path);
        return path.toString();
    }

    /**
     * Sanity Check: If the internal ID contains a pathname separator, it's probably
     * an attempt to make a path traversal attack, so ignore the path prefix. The
     * internal-ID is supposed to be just a filename, so this will not affect normal
     * operation.
     * 
     * @param sInternalId
     * @return Sanitized id
     */
    protected String sanitizeIdentifier(String sInternalId) {
        if (sInternalId.contains(File.separator)) {
            sInternalId = sInternalId.substring(sInternalId.lastIndexOf(File.separator) + 1);
        }
        return sInternalId;
    }

    /**
     * Append separator to target {@code StringBuilder}
     * 
     * @param path
     */
    protected void appendSeparator(StringBuilder path) {
        if (!endsWithSeparator(path)) {
            path.append(File.separator);
        }
    }

    /**
     * Utility that checks string ending with separator
     * 
     * @param bufFilename
     * @return
     */
    protected boolean endsWithSeparator(StringBuilder bufFilename) {
        return bufFilename.lastIndexOf(File.separator) == bufFilename.length() - 1;
    }

    /**
     * Splits internalId into several subpaths using {@code digitsPerLevel} that
     * indicates the folder name length, and {@code direcoryLevels} that indicates
     * the maximum number of subfolders.
     * 
     * @param internalId bitStream identifier
     * @param path
     */
    protected void populatePathSplittingId(String internalId, StringBuilder path) {
        int digits = 0;
        path.append(extractSubstringFrom(internalId, digits, digits + digitsPerLevel));
        for (int i = 1; i < directoryLevels && !isLonger(internalId, digits + digitsPerLevel); i++) {
            digits = i * digitsPerLevel;
            path.append(File.separator);
            path.append(extractSubstringFrom(internalId, digits, digits + digitsPerLevel));
        }
    }

    /**
     * Extract substring if is in range, otherwise will truncate to length
     * 
     * @param internalId
     * @param startIndex
     * @param endIndex
     * @return
     */
    protected String extractSubstringFrom(String internalId, int startIndex, int endIndex) {
        if (isLonger(internalId, endIndex)) {
            endIndex = internalId.length();
        }
        return internalId.substring(startIndex, endIndex);
    }

    /**
     * Checks if the {@code String} is longer than {@code endIndex}
     * 
     * @param internalId
     * @param endIndex
     * @return
     */
    protected boolean isLonger(String internalId, int endIndex) {
        return endIndex > internalId.length();
    }

    /**
     * Retrieves a map of useful metadata about the File (size, checksum, modified)
     * 
     * @param file The File to analyze
     * @param attrs The list of requested metadata values
     * @return Map of updated metadatas / attrs
     * @throws IOException
     */
    public Map<String, Object> about(File file, List<String> attrs) throws IOException {

        Map<String, Object> metadata = new HashMap<String, Object>();

        try {
            if (file != null && file.exists()) {
                this.putValueIfExistsKey(attrs, metadata, SIZE_BYTES, file.length());
                if (attrs.contains(CHECKSUM)) {
                    metadata.put(CHECKSUM, Utils.toHex(this.generateChecksumFrom(file)));
                    metadata.put(CHECKSUM_ALGORITHM, CSA);
                }
                this.putValueIfExistsKey(attrs, metadata, MODIFIED, String.valueOf(file.lastModified()));
            }
            return metadata;
        } catch (Exception e) {
            log.error("about( FilePath: " + file.getAbsolutePath() + ", Map: " + attrs.toString() + ")", e);
            throw new IOException(e);
        }
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    private byte[] generateChecksumFrom(File file) throws FileNotFoundException, IOException {
        // generate checksum by reading the bytes
        try (FileInputStream fis = new FileInputStream(file)) {
            return generateChecksumFrom(fis);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Caught NoSuchAlgorithmException", e);
            throw new IOException("Invalid checksum algorithm");
        }
    }

    private byte[] generateChecksumFrom(FileInputStream fis) throws IOException, NoSuchAlgorithmException {
        try (DigestInputStream dis = new DigestInputStream(fis, MessageDigest.getInstance(CSA))) {
            final int BUFFER_SIZE = 1024 * 4;
            final byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                final int count = dis.read(buffer, 0, BUFFER_SIZE);
                if (count == -1) {
                    break;
                }
            }
            return dis.getMessageDigest().digest();
        }
    }

    protected void putValueIfExistsKey(List<String> attrs, Map<String, Object> metadata, String key, Object value) {
        if (attrs.contains(key)) {
            metadata.put(key, value);
        }
    }

}
