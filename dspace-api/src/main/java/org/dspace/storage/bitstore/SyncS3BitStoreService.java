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
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
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

    /**
     * The uploading file is divided into parts and each part is uploaded separately. The size of the part is 50 MB.
     */
    private static final long UPLOAD_FILE_PART_SIZE = 50 * 1024 * 1024; // 50 MB

    /**
     * Upload large file by parts - check the checksum of every part
     */
    private boolean uploadByParts = false;

    @Autowired(required = true)
    DSBitStoreService dsBitStoreService;

    @Autowired(required = true)
    ConfigurationService configurationService;

    public SyncS3BitStoreService() {
        super();
    }

    /**
     * Define syncEnabled and uploadByParts in the constructor - this values won't be overridden by the configuration
     *
     * @param syncEnabled   if true, the file will be uploaded to the local assetstore
     * @param uploadByParts if true, the file will be uploaded by parts
     */
    public SyncS3BitStoreService(boolean syncEnabled, boolean uploadByParts) {
        super();
        this.syncEnabled = syncEnabled;
        this.uploadByParts = uploadByParts;
    }

    public void init() throws IOException {
        super.init();

        // The syncEnabled and uploadByParts could be set to true in the constructor,
        // do not override them by the configuration in this case
        if (!syncEnabled) {
            syncEnabled = configurationService.getBooleanProperty("sync.storage.service.enabled", false);
        }
        if (!uploadByParts) {
            uploadByParts = configurationService.getBooleanProperty("s3.upload.by.parts.enabled", false);
        }
    }

    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(bitstream.getInternalId(), "s3bs");
        try (
                FileOutputStream fos = new FileOutputStream(scratchFile);
                // Read through a digest input stream that will work out the MD5
                DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));
        ) {
            Utils.bufferedCopy(dis, fos);
            in.close();

            if (uploadByParts) {
                uploadByParts(key, scratchFile);
            } else {
                uploadFluently(key, scratchFile);
            }

            bitstream.setSizeBytes(scratchFile.length());
            // we cannot use the S3 ETAG here as it could be not a MD5 in case of multipart upload (large files) or if
            // the bucket is encrypted
            bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
            bitstream.setChecksumAlgorithm(CSA);

            if (syncEnabled) {
                // Upload file into local assetstore - use buffered copy to avoid memory issues, because of large files
                File localFile = dsBitStoreService.getFile(bitstream);
                // Create a new file in the assetstore if it does not exist
                createFileIfNotExist(localFile);

                // Copy content from scratch file to local assetstore file
                FileInputStream fisScratchFile =  new FileInputStream(scratchFile);
                FileOutputStream fosLocalFile = new FileOutputStream(localFile);
                Utils.bufferedCopy(fisScratchFile, fosLocalFile);
                fisScratchFile.close();
            }
        } catch (AmazonClientException | IOException | InterruptedException e) {
            log.error("put(" + bitstream.getInternalId() + ", is)", e);
            throw new IOException(e);
        } catch (NoSuchAlgorithmException nsae) {
            // Should never happen
            log.warn("Caught NoSuchAlgorithmException", nsae);
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

    /**
     * Create a new file in the assetstore if it does not exist
     *
     * @param localFile
     * @throws IOException
     */
    private void createFileIfNotExist(File localFile) throws IOException {
        if (localFile.exists()) {
            return;
        }

        // Create the necessary parent directories if they do not yet exist
        if (!localFile.getParentFile().mkdirs()) {
            throw new IOException("Assetstore synchronization error: Directories in the assetstore for the file " +
                    "with path" + localFile.getParent() + " were not created");
        }
        if (!localFile.createNewFile()) {
            throw new IOException("Assetstore synchronization error: File " + localFile.getPath() +
                    " was not created");
        }
    }

    /**
     * Upload a file fluently. The file is uploaded in a single request.
     *
     * @param key the bitstream's internalId
     * @param scratchFile the file to upload
     * @throws InterruptedException if the S3 upload is interrupted
     */
    private void uploadFluently(String key, File scratchFile) throws InterruptedException {
        Upload upload = tm.upload(getBucketName(), key, scratchFile);

        upload.waitForUploadResult();
    }

    /**
     * Upload a file by parts. The file is divided into parts and each part is uploaded separately.
     * The checksum of each part is checked. If the checksum does not match, the file is not uploaded.
     *
     * @param key the bitstream's internalId
     * @param scratchFile the file to upload
     * @throws IOException if an I/O error occurs
     */
    private void uploadByParts(String key, File scratchFile) throws IOException {
        // Initialize MessageDigest for computing checksum
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }

        // Initiate multipart upload
        InitiateMultipartUploadRequest initiateRequest = new InitiateMultipartUploadRequest(getBucketName(), key);
        String uploadId = this.s3Service.initiateMultipartUpload(initiateRequest).getUploadId();

        // Create a list to hold the ETags for individual parts
        List<PartETag> partETags = new ArrayList<>();

        try {
            // Upload parts
            File file = new File(scratchFile.getPath());
            long fileLength = file.length();
            long remainingBytes = fileLength;
            int partNumber = 1;

            while (remainingBytes > 0) {
                long bytesToUpload = Math.min(UPLOAD_FILE_PART_SIZE, remainingBytes);

                // Calculate the checksum for the part
                String partChecksum = calculatePartChecksum(file, fileLength - remainingBytes, bytesToUpload, digest);

                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(this.getBucketName())
                        .withKey(key)
                        .withUploadId(uploadId)
                        .withPartNumber(partNumber)
                        .withFile(file)
                        .withFileOffset(fileLength - remainingBytes)
                        .withPartSize(bytesToUpload);

                // Upload the part
                UploadPartResult uploadPartResponse = this.s3Service.uploadPart(uploadRequest);

                // Collect the ETag for the part
                partETags.add(uploadPartResponse.getPartETag());

                // Compare checksums - local with ETag
                if (!StringUtils.equals(uploadPartResponse.getETag(), partChecksum)) {
                    String errorMessage = "Checksums do not match error: The locally computed checksum does " +
                            "not match with the ETag from the UploadPartResult. Local checksum: " + partChecksum +
                            ", ETag: " + uploadPartResponse.getETag() + ", partNumber: " + partNumber;
                    log.error(errorMessage);
                    throw new IOException(errorMessage);
                }

                remainingBytes -= bytesToUpload;
                partNumber++;
            }

            // Complete the multipart upload
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(this.getBucketName(),
                    key, uploadId, partETags);
            this.s3Service.completeMultipartUpload(completeRequest);
        } catch (AmazonClientException e) {
            log.error("Cannot upload the file by parts because: ", e);
        }
    }

    /**
     * Calculate the checksum of the specified part of the file (Multipart upload)
     *
     * @param file the uploading file
     * @param offset the offset in the file
     * @param length the length of the part
     * @param digest the message digest for computing the checksum
     * @return the checksum of the part
     * @throws IOException if an I/O error occurs
     */
    public static String calculatePartChecksum(File file, long offset, long length, MessageDigest digest)
            throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(fis, digest)) {
            // Skip to the specified offset
            fis.skip(offset);

            // Read the specified length
            IOUtils.copyLarge(dis, OutputStream.nullOutputStream(), 0, length);

            // Convert the digest to a hex string
            return Utils.toHex(digest.digest());
        }
    }
}
