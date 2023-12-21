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

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Override of the S3BitStoreService to store all the data also in the local assetstore.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class SyncS3BitStoreService extends S3BitStoreService {

    /**
     * log4j log
     */
    private static final Logger log = LogManager.getLogger(SyncS3BitStoreService.class);
    private boolean syncEnabled = false;

    @Autowired(required = true)
    DSBitStoreService dsBitStoreService;

    @Autowired(required = true)
    ConfigurationService configurationService;

    public SyncS3BitStoreService() {
        super();
    }

    public void init() throws IOException {
        super.init();
        syncEnabled = configurationService.getBooleanProperty("sync.storage.service.enabled", false);
    }

    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(bitstream.getInternalId(), "s3bs");
        try {
            FileUtils.copyInputStreamToFile(in, scratchFile);
            long contentLength = scratchFile.length();
            // The ETag may or may not be and MD5 digest of the object data.
            // Therefore, we precalculate before uploading
            String localChecksum = org.dspace.curate.Utils.checksum(scratchFile, CSA);

            Upload upload = tm.upload(getBucketName(), key, scratchFile);

            upload.waitForUploadResult();

            bitstream.setSizeBytes(contentLength);
            bitstream.setChecksum(localChecksum);
            bitstream.setChecksumAlgorithm(CSA);

            if (syncEnabled) {
                // Upload file into local assetstore
                File localFile = dsBitStoreService.getFile(bitstream);
                FileUtils.copyFile(scratchFile, localFile);
            }
        } catch (AmazonClientException | IOException | InterruptedException e) {
            log.error("put(" + bitstream.getInternalId() + ", is)", e);
            throw new IOException(e);
        } finally {
            if (!scratchFile.delete()) {
                scratchFile.deleteOnExit();
            }
        }
    }

    @Override
    public void remove(Bitstream bitstream) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        try {
            // Remove file from S3
            s3Service.deleteObject(getBucketName(), key);
            if (syncEnabled) {
                // Remove file from local assetstore
                dsBitStoreService.remove(bitstream);
            }
        } catch (AmazonClientException e) {
            log.error("remove(" + key + ")", e);
            throw new IOException(e);
        }
    }
}
