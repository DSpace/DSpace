/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static java.lang.String.valueOf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.util.FunctionalUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Asset store using Amazon's Simple Storage Service (S3).
 * S3 is a commercial, web-service accessible, remote storage facility.
 * NB: you must have obtained an account with Amazon to use this store
 *
 * @author Richard Rodgers, Peter Dietz
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */

public class S3BitStoreService extends BaseBitStoreService {
    protected static final String DEFAULT_BUCKET_PREFIX = "dspace-asset-";
    // Prefix indicating a registered bitstream
    protected final String REGISTERED_FLAG = "-R";
    /**
     * log4j log
     */
    private static final Logger log = LogManager.getLogger(S3BitStoreService.class);

    /**
     * Checksum algorithm
     */
    static final String CSA = "MD5";

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

    private boolean enabled = false;

    private String awsAccessKey;
    private String awsSecretKey;
    private String awsRegionName;
    private boolean useRelativePath;

    /**
     * The maximum size of individual chunk to download from S3 when a file is accessed. Default 5Mb
     */
    private long bufferSize = 5 * 1024 * 1024;

    /**
     * container for all the assets
     */
    private String bucketName = null;

    /**
     * (Optional) subfolder within bucket where objects are stored
     */
    private String subfolder = null;

    /**
     * S3 service
     */
    private AmazonS3 s3Service = null;

    /**
     * S3 transfer manager
     * this is reused between put calls to use less resources for multiple uploads
     */
    private TransferManager tm = null;

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Utility method for generate AmazonS3 builder
     *
     * @param regions wanted regions in client
     * @param awsCredentials credentials of the client
     * @return builder with the specified parameters
     */
    protected static Supplier<AmazonS3> amazonClientBuilderBy(
            @NotNull Regions regions,
            @NotNull AWSCredentials awsCredentials
    ) {
        return () -> AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(regions)
                .build();
    }

    public S3BitStoreService() {}

    /**
     * This constructor is used for test purpose.
     *
     * @param s3Service AmazonS3 service
     */
    protected S3BitStoreService(AmazonS3 s3Service) {
        this.s3Service = s3Service;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Initialize the asset store
     * S3 Requires:
     * - access key
     * - secret key
     * - bucket name
     */
    @Override
    public void init() throws IOException {

        if (this.isInitialized() || !this.isEnabled()) {
            return;
        }

        try {
            if (StringUtils.isNotBlank(getAwsAccessKey()) && StringUtils.isNotBlank(getAwsSecretKey())) {
                log.warn("Use local defined S3 credentials");
                // region
                Regions regions = Regions.DEFAULT_REGION;
                if (StringUtils.isNotBlank(awsRegionName)) {
                    try {
                        regions = Regions.fromName(awsRegionName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid aws_region: " + awsRegionName);
                    }
                }
                // init client
                s3Service = FunctionalUtils.getDefaultOrBuild(
                        this.s3Service,
                        amazonClientBuilderBy(
                                regions,
                                new BasicAWSCredentials(getAwsAccessKey(), getAwsSecretKey())
                                )
                        );
                log.warn("S3 Region set to: " + regions.getName());
            } else {
                log.info("Using a IAM role or aws environment credentials");
                s3Service = FunctionalUtils.getDefaultOrBuild(
                        this.s3Service,
                        AmazonS3ClientBuilder::defaultClient
                        );
            }

            // bucket name
            if (StringUtils.isEmpty(bucketName)) {
                // get hostname of DSpace UI to use to name bucket
                String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
                bucketName = DEFAULT_BUCKET_PREFIX + hostname;
                log.warn("S3 BucketName is not configured, setting default: " + bucketName);
            }

            try {
                if (!s3Service.doesBucketExistV2(bucketName)) {
                    s3Service.createBucket(bucketName);
                    log.info("Creating new S3 Bucket: " + bucketName);
                }
            } catch (AmazonClientException e) {
                throw new IOException(e);
            }
            this.initialized = true;
            log.info("AWS S3 Assetstore ready to go! bucket:" + bucketName);
        } catch (Exception e) {
            this.initialized = false;
            log.error("Can't initialize this store!", e);
        }

        log.info("AWS S3 Assetstore ready to go! bucket:" + bucketName);

        tm = FunctionalUtils.getDefaultOrBuild(tm, () -> TransferManagerBuilder.standard()
                                                               .withAlwaysCalculateMultipartMd5(true)
                                                               .withS3Client(s3Service)
                                                               .build());
    }

    /**
     * Return an identifier unique to this asset store instance
     *
     * @return a unique ID
     */
    @Override
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
    @Override
    public InputStream get(Bitstream bitstream) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        // Strip -R from bitstream key if it's registered
        if (isRegisteredBitstream(key)) {
            key = key.substring(REGISTERED_FLAG.length());
        }
        return new S3LazyInputStream(key, bufferSize, bitstream.getSizeBytes());
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

            Upload upload = tm.upload(bucketName, key, scratchFile);

            upload.waitForUploadResult();

            bitstream.setSizeBytes(scratchFile.length());
            // we cannot use the S3 ETAG here as it could be not a MD5 in case of multipart upload (large files) or if
            // the bucket is encrypted
            bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
            bitstream.setChecksumAlgorithm(CSA);

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

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * Checksum used is (ETag) hex encoded 128-bit MD5 digest of an object's content as calculated by Amazon S3
     * (Does not use getContentMD5, as that is 128-bit MD5 digest calculated on caller's side)
     *
     * @param bitstream The asset to describe
     * @param attrs     A List of desired metadata fields
     * @return attrs
     * A Map with key/value pairs of desired metadata
     * If file not found, then return null
     * @throws java.io.IOException If a problem occurs while obtaining metadata
     */
    @Override
    public Map<String, Object> about(Bitstream bitstream, List<String> attrs) throws IOException {

        String key = getFullKey(bitstream.getInternalId());
        // If this is a registered bitstream, strip the -R prefix before retrieving
        if (isRegisteredBitstream(key)) {
            key = key.substring(REGISTERED_FLAG.length());
        }

        Map<String, Object> metadata = new HashMap<>();

        try {

            ObjectMetadata objectMetadata = s3Service.getObjectMetadata(bucketName, key);
            if (objectMetadata != null) {
                putValueIfExistsKey(attrs, metadata, "size_bytes", objectMetadata.getContentLength());
                putValueIfExistsKey(attrs, metadata, "modified", valueOf(objectMetadata.getLastModified().getTime()));
            }

            putValueIfExistsKey(attrs, metadata, "checksum_algorithm", CSA);

            if (attrs.contains("checksum")) {
                try (InputStream in = get(bitstream);
                     DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA))
                ) {
                    Utils.copy(dis, NullOutputStream.NULL_OUTPUT_STREAM);
                    byte[] md5Digest = dis.getMessageDigest().digest();
                    metadata.put("checksum", Utils.toHex(md5Digest));
                } catch (NoSuchAlgorithmException nsae) {
                    // Should never happen
                    log.warn("Caught NoSuchAlgorithmException", nsae);
                }
            }

            return metadata;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return metadata;
            }
        } catch (AmazonClientException e) {
            log.error("about(" + key + ", attrs)", e);
            throw new IOException(e);
        }
        return metadata;
    }

    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param bitstream The asset to delete
     * @throws java.io.IOException If a problem occurs while removing the asset
     */
    @Override
    public void remove(Bitstream bitstream) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        try {
            s3Service.deleteObject(bucketName, key);
        } catch (AmazonClientException e) {
            log.error("remove(" + key + ")", e);
            throw new IOException(e);
        }
    }

    /**
     * Utility Method: Prefix the key with a subfolder, if this instance assets are stored within subfolder
     *
     * @param id DSpace bitstream internal ID
     * @return full key prefixed with a subfolder, if applicable
     */
    public String getFullKey(String id) {
        StringBuilder bufFilename = new StringBuilder();
        if (StringUtils.isNotEmpty(subfolder)) {
            bufFilename.append(subfolder);
            appendSeparator(bufFilename);
        }

        if (this.useRelativePath) {
            bufFilename.append(getRelativePath(id));
        } else {
            bufFilename.append(id);
        }

        if (log.isDebugEnabled()) {
            log.debug("S3 filepath for " + id + " is "
                    + bufFilename.toString());
        }

        return bufFilename.toString();
    }

    /**
     * there are 2 cases:
     * - conventional bitstream, conventional storage
     * - registered bitstream, conventional storage
     *  conventional bitstream: dspace ingested, dspace random name/path
     *  registered bitstream: registered to dspace, any name/path
     *
     * @param sInternalId
     * @return Computed Relative path
     */
    public String getRelativePath(String sInternalId) {
        BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance()
                .getBitstreamStorageService();

        String sIntermediatePath = StringUtils.EMPTY;
        if (bitstreamStorageService.isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(REGISTERED_FLAG.length());
        } else {
            sInternalId = sanitizeIdentifier(sInternalId);
            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        return sIntermediatePath + sInternalId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    @Autowired(required = true)
    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    @Autowired(required = true)
    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsRegionName() {
        return awsRegionName;
    }

    public void setAwsRegionName(String awsRegionName) {
        this.awsRegionName = awsRegionName;
    }

    @Autowired(required = true)
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getSubfolder() {
        return subfolder;
    }

    public void setSubfolder(String subfolder) {
        this.subfolder = subfolder;
    }

    public boolean isUseRelativePath() {
        return useRelativePath;
    }

    public void setUseRelativePath(boolean useRelativePath) {
        this.useRelativePath = useRelativePath;
    }

    /**
     * Contains a command-line testing tool. Expects arguments:
     * -a accessKey -s secretKey -f assetFileName
     *
     * @param args the command line arguments given
     * @throws Exception generic exception
     */
    public static void main(String[] args) throws Exception {
        //TODO Perhaps refactor to be a unit test. Can't mock this without keys though.

        // parse command line
        Options options = new Options();
        Option option;

        option = Option.builder("a").desc("access key").hasArg().required().build();
        options.addOption(option);

        option = Option.builder("s").desc("secret key").hasArg().required().build();
        options.addOption(option);

        option = Option.builder("f").desc("asset file name").hasArg().required().build();
        options.addOption(option);

        DefaultParser parser = new DefaultParser();

        CommandLine command;
        try {
            command = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            new HelpFormatter().printHelp(
                    S3BitStoreService.class.getSimpleName() + "options", options);
            return;
        }

        String accessKey = command.getOptionValue("a");
        String secretKey = command.getOptionValue("s");

        S3BitStoreService store = new S3BitStoreService();

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        store.s3Service = AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
            .build();

        //Todo configurable region
        Region usEast1 = Region.getRegion(Regions.US_EAST_1);
        store.s3Service.setRegion(usEast1);

        // get hostname of DSpace UI to use to name bucket
        String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
        //Bucketname should be lowercase
        store.bucketName = DEFAULT_BUCKET_PREFIX + hostname + ".s3test";
        store.s3Service.createBucket(store.bucketName);
        /* Broken in DSpace 6 TODO Refactor
        // time everything, todo, switch to caliper
        long start = System.currentTimeMillis();
        // Case 1: store a file
        String id = store.generateId();
        System.out.print("put() file " + assetFile + " under ID " + id + ": ");
        FileInputStream fis = new FileInputStream(assetFile);
        //TODO create bitstream for assetfile...
        Map attrs = store.put(fis, id);
        long now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        // examine the metadata returned
        Iterator iter = attrs.keySet().iterator();
        System.out.println("Metadata after put():");
        while (iter.hasNext())
        {
            String key = (String)iter.next();
            System.out.println( key + ": " + (String)attrs.get(key) );
        }
        // Case 2: get metadata and compare
        System.out.print("about() file with ID " + id + ": ");
        Map attrs2 = store.about(id, attrs);
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        iter = attrs2.keySet().iterator();
        System.out.println("Metadata after about():");
        while (iter.hasNext())
        {
            String key = (String)iter.next();
            System.out.println( key + ": " + (String)attrs.get(key) );
        }
        // Case 3: retrieve asset and compare bits
        System.out.print("get() file with ID " + id + ": ");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(assetFile+".echo");
        InputStream in = store.get(id);
        Utils.bufferedCopy(in, fos);
        fos.close();
        in.close();
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        // Case 4: remove asset
        System.out.print("remove() file with ID: " + id + ": ");
        store.remove(id);
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        System.out.flush();
        // should get nothing back now - will throw exception
        store.get(id);
*/
    }

    /**
     * Is this a registered bitstream? (not stored via this service originally)
     * @param internalId
     * @return
     */
    public boolean isRegisteredBitstream(String internalId) {
        return internalId.startsWith(REGISTERED_FLAG);
    }

    public void setBufferSize(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * This inner class represent an InputStream that uses temporary files to
     * represent chunk of the object downloaded from S3. When the input stream is
     * read the class look first to the current chunk and download a new one once if
     * the current one as been fully read. The class is responsible to close a chunk
     * as soon as a new one is retrieved, the last chunk is closed when the input
     * stream itself is closed or the last byte is read (the first of the two)
     */
    public class S3LazyInputStream extends InputStream {
        private InputStream currentChunkStream;
        private String objectKey;
        private long endOfChunk = -1;
        private long chunkMaxSize;
        private long currPos = 0;
        private long fileSize;

        public S3LazyInputStream(String objectKey, long chunkMaxSize, long fileSize) throws IOException {
            this.objectKey = objectKey;
            this.chunkMaxSize = chunkMaxSize;
            this.endOfChunk = 0;
            this.fileSize = fileSize;
            downloadChunk();
        }

        @Override
        public int read() throws IOException {
            // is the current chunk completely read and other are available?
            if (currPos == endOfChunk && currPos < fileSize) {
                currentChunkStream.close();
                downloadChunk();
            }

            int byteRead = currPos < endOfChunk ? currentChunkStream.read() : -1;
            // do we get any data or are we at the end of the file?
            if (byteRead != -1) {
                currPos++;
            } else {
                currentChunkStream.close();
            }
            return byteRead;
        }

        /**
         * This method download the next chunk from S3
         *
         * @throws IOException
         * @throws FileNotFoundException
         */
        private void downloadChunk() throws IOException, FileNotFoundException {
            // Create a DownloadFileRequest with the desired byte range
            long startByte = currPos; // Start byte (inclusive)
            long endByte = Long.min(startByte + chunkMaxSize - 1, fileSize - 1); // End byte (inclusive)
            GetObjectRequest getRequest = new GetObjectRequest(bucketName, objectKey)
                    .withRange(startByte, endByte);

            File currentChunkFile = File.createTempFile("s3-disk-copy-" + UUID.randomUUID(), "temp");
            currentChunkFile.deleteOnExit();
            try {
                Download download = tm.download(getRequest, currentChunkFile);
                download.waitForCompletion();
                currentChunkStream = new DeleteOnCloseFileInputStream(currentChunkFile);
                endOfChunk = endOfChunk + download.getProgress().getBytesTransferred();
            } catch (AmazonClientException | InterruptedException e) {
                currentChunkFile.delete();
                throw new IOException(e);
            }
        }

        @Override
        public void close() throws IOException {
            if (currentChunkStream != null) {
                currentChunkStream.close();
            }
        }

    }
}
