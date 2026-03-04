/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static java.lang.String.valueOf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Asset store using Amazon's Simple Storage Service (S3).
 * S3 is a commercial, web-service accessible, remote storage facility.
 * NB: you must have obtained an account with Amazon to use this store
 *
 * @author Richard Rodgers, Peter Dietz
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 * @author Mark Patton
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

    private boolean enabled = false;

    /**
     *  Override AWS endpoint if not null
     */
    private String endpoint = null;

    private String awsAccessKey;
    private String awsSecretKey;
    private String awsRegionName;
    private boolean useRelativePath;
    private double targetThroughputGbps = 10.0;
    private long minPartSizeBytes = 8 * 1024 * 1024L;
    private ChecksumAlgorithm s3ChecksumAlgorithm = ChecksumAlgorithm.CRC32;
    private Integer maxConcurrency = null;

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
    private S3AsyncClient s3AsyncClient = null;

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Utility method for generate AmazonS3 builder
     *
     * @param regions wanted regions in client
     * @param awsCredentials credentials of the client
     * @param endpoint custom AWS endpoint
     * @param targetThroughput target throughput in Gbps
     * @param minPartSize minimum part size in bytes
     * @param maxConcurrency maximum number of concurrent requests
     * @return builder with the specified parameters
     */
    protected static Supplier<S3AsyncClient> amazonClientBuilderBy(
            Region region,
            AwsCredentialsProvider credentialsProvider,
            String endpoint,
            double targetThroughput,
            long minPartSize,
            Integer maxConcurrency
    ) {
        return () -> {
            S3CrtAsyncClientBuilder crtBuilder = S3AsyncClient.crtBuilder();

            if (credentialsProvider != null) {
                crtBuilder.credentialsProvider(credentialsProvider);
            }

            if (region != null) {
                crtBuilder.region(region);
            }

            if (maxConcurrency != null) {
                crtBuilder.maxConcurrency(maxConcurrency);
            }

            if (StringUtils.isNotBlank(endpoint)) {
                crtBuilder.endpointOverride(URI.create(endpoint));
                crtBuilder.forcePathStyle(true);
            }

            return crtBuilder.targetThroughputInGbps(targetThroughput).minimumPartSizeInBytes(minPartSize).build();
        };
    }

    public S3BitStoreService() {}

    /**
     * This constructor is used for test purpose.
     *
     * @param s3AsyncClient AmazonS3 service
     */
    protected S3BitStoreService(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
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
                Region region = Region.US_EAST_1;
                if (StringUtils.isNotBlank(awsRegionName)) {
                    try {
                        region = Region.of(awsRegionName);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid aws_region: " + awsRegionName);
                    }
                }

                // init client
                s3AsyncClient = FunctionalUtils.getDefaultOrBuild(
                        this.s3AsyncClient,
                        amazonClientBuilderBy(
                                region,
                                StaticCredentialsProvider.create(AwsBasicCredentials.create(getAwsAccessKey(),
                                        getAwsSecretKey())), endpoint, targetThroughputGbps,
                                minPartSizeBytes, maxConcurrency)
                        );
                log.warn("S3 Region set to: " + region.id());
            } else {
                log.info("Using a IAM role or aws environment credentials");
                s3AsyncClient = FunctionalUtils.getDefaultOrBuild(
                        this.s3AsyncClient,
                        amazonClientBuilderBy(null, null , endpoint, targetThroughputGbps,
                                minPartSizeBytes, maxConcurrency));
            }

            // bucket name
            if (StringUtils.isEmpty(bucketName)) {
                // get hostname of DSpace UI to use to name bucket
                String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
                bucketName = DEFAULT_BUCKET_PREFIX + hostname;
                log.warn("S3 BucketName is not configured, setting default: " + bucketName);
            }


            if (!doesBucketExist(bucketName)) {
                s3AsyncClient.createBucket(r -> r.bucket(bucketName)).join();
                log.info("Creating new S3 Bucket: " + bucketName);
            }
            this.initialized = true;
            log.info("AWS S3 Assetstore ready to go! bucket:" + bucketName);
        } catch (Exception e) {
            this.initialized = false;
            log.error("Can't initialize this store!", e);
        }
    }

    /**
     * @param bucketName
     * @return whether or not the specified bucket exists
     */
    public boolean doesBucketExist(String bucketName ) {
        try {
            s3AsyncClient.headBucket(r -> r.bucket(bucketName)).join();
            return true;
        } catch (CompletionException ce) {
            if (!(ce.getCause() instanceof NoSuchBucketException)) {
                log.error("headBucket(" + bucketName + ")", ce.getCause());
            }

            return false;
        }
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

        final String objectKey = key;

        try {
            return s3AsyncClient.getObject(r -> r.bucket(bucketName).key(objectKey),
                AsyncResponseTransformer.toBlockingInputStream()).join();
        } catch (CompletionException e) {
            throw new IOException(e.getCause());
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
    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA))) {
            AsyncRequestBody body = AsyncRequestBody.fromInputStream(dis, null, executor);

            s3AsyncClient.putObject(b ->  b.bucket(bucketName).key(key).checksumAlgorithm(s3ChecksumAlgorithm),
                    body).join();

            bitstream.setSizeBytes(s3AsyncClient.headObject(r -> r.bucket(bucketName).key(key))
                    .join().contentLength());

            // we cannot use the S3 ETAG here as it could be not a MD5 in case of multipart upload (large files) or if
            // the bucket is encrypted
            bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
            bitstream.setChecksumAlgorithm(CSA);
        } catch (CompletionException e) {
            log.error("put(" + bitstream.getInternalId() + ", is)", e.getCause());
            throw new IOException(e.getCause());
        } catch (IOException e) {
            log.error("put(" + bitstream.getInternalId() + ", is)", e);
            throw new IOException(e);
        } catch (NoSuchAlgorithmException nsae) {
            // Should never happen
            log.warn("Caught NoSuchAlgorithmException", nsae);
        } finally {
            executor.shutdown();
            in.close();
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * The MD5 checksum is calculated locally because it is not supported by AWS.
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
            final String objectKey = key;
            HeadObjectResponse response = s3AsyncClient.headObject(r -> r.bucket(bucketName).key(objectKey)).join();

            putValueIfExistsKey(attrs, metadata, "size_bytes", response.contentLength());
            putValueIfExistsKey(attrs, metadata, "modified", valueOf(response.lastModified().toEpochMilli()));
            putValueIfExistsKey(attrs, metadata, "checksum_algorithm", CSA);

            if (attrs.contains("checksum")) {
                try (InputStream in = get(bitstream);
                     DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA))
                ) {
                    Utils.copy(dis, NullOutputStream.INSTANCE);
                    byte[] md5Digest = dis.getMessageDigest().digest();
                    metadata.put("checksum", Utils.toHex(md5Digest));
                } catch (NoSuchAlgorithmException nsae) {
                    // Should never happen
                    log.warn("Caught NoSuchAlgorithmException", nsae);
                }
            }

            return metadata;
        } catch (CompletionException e) {
            if (e.getCause() instanceof AwsServiceException awsEx &&
                    awsEx.statusCode() == HttpStatusCode.NOT_FOUND) {
                return metadata;
            }

            log.error("about(" + key + ", attrs)", e);
            throw new IOException(e);
        }
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
            s3AsyncClient.deleteObject(r -> r.bucket(bucketName).key(key)).join();
        }  catch (CompletionException e) {
            log.error("remove(" + key + ")", e.getCause());
            throw new IOException(e.getCause());
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

    public double getTargetThroughputGbps() {
        return targetThroughputGbps;
    }

    public void setTargetThroughputGbps(double targetThroughputGbps) {
        this.targetThroughputGbps = targetThroughputGbps;
    }

    public long getMinPartSizeBytes() {
        return minPartSizeBytes;
    }

    public void setMinPartSizeBytes(long minPartSizeBytes) {
        this.minPartSizeBytes = minPartSizeBytes;
    }

    public ChecksumAlgorithm getS3ChecksumAlgorithm() {
        return s3ChecksumAlgorithm;
    }

    public void setS3ChecksumAlgorithm(ChecksumAlgorithm s3ChecksumAlgorithm) {
        this.s3ChecksumAlgorithm = s3ChecksumAlgorithm;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        // Todo configurable region
        store.s3AsyncClient = S3AsyncClient.builder().credentialsProvider(credentialsProvider).
                                region(Region.US_EAST_1).build();

        // get hostname of DSpace UI to use to name bucket
        String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
        //Bucketname should be lowercase
        store.bucketName = DEFAULT_BUCKET_PREFIX + hostname + ".s3test";
        store.s3AsyncClient.createBucket(r -> r.bucket(store.bucketName)).join();
    }

    /**
     * Is this a registered bitstream? (not stored via this service originally)
     * @param internalId
     * @return
     */
    public boolean isRegisteredBitstream(String internalId) {
        return internalId.startsWith(REGISTERED_FLAG);
    }
}
