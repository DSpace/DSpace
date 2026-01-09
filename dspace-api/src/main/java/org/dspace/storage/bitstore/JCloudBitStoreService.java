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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.blobstore.options.PutOptions.Builder;
import org.jclouds.io.ContentMetadata;
import org.jclouds.javax.annotation.Nullable;

/**
 * JCloudBitstream asset store service
 *
 * This class provides an implementation of the BitstreamStorageService using JClouds for cloud storage.
 * It supports storing, retrieving, and removing bitstreams in a cloud storage container.
 * The class also handles initialization and configuration of the JClouds BlobStoreContext.
 *
 * Additional details regarding configuration can be found at:
 * Blobstore Configuration https://jclouds.apache.org/start/blobstore/
 * Providers and API https://jclouds.apache.org/reference/providers/#blobstore
 *
 * Author: Mark Diggory, Nathan Buckingham
 */
public class JCloudBitStoreService extends BaseBitStoreService {

    /** Logger for this class */
    private static final Logger log = LogManager.getLogger(JCloudBitStoreService.class);

    /** Properties for configuring the cloud storage provider */
    private Properties properties;

    /** The cloud storage provider or API to use (e.g. "aws-s3", "openstack-swift") */
    private String providerOrApi;

    /** JClouds ContextBuilder for creating the storage context */
    private ContextBuilder builder;

    /** JClouds BlobStoreContext for interacting with the cloud storage */
    private BlobStoreContext blobStoreContext;

    /**
     * Container/bucket name where assets are stored.
     * Required for cloud storage providers.
     */
    private String container;

    /**
     * Optional subfolder path within the container/bucket.
     * Allows organizing assets in a subfolder hierarchy.
     */
    private String subfolder = null;

    /** Authentication identity/access key for the cloud provider */
    private String identity;

    /** Authentication credential/secret key for the cloud provider */
    private String credential;

    /** Optional endpoint URL for the cloud storage service */
    private String endpoint;

    /** Whether to use relative paths when storing assets */
    private boolean useRelativePath;

    /** Whether this storage service is enabled */
    private boolean enabled = false;

    /** Counter for tracking context refreshes */
    private int counter = 0;

    /** Maximum value for context refresh counter before forcing refresh */
    private int maxCounter = -1;

    /** Checksum algorithm used (MD5) */
    private static final String CSA = "MD5";

    /**
     * Default constructor.
     */
    public JCloudBitStoreService() {
    }

    /**
     * Constructor with provider or API.
     *
     * @param providerOrApi the provider or API to use for cloud storage
     */
    public JCloudBitStoreService(String providerOrApi) {
        this.providerOrApi = providerOrApi;
    }

    /**
     * Protected constructor with BlobStoreContext and provider or API.
     *
     * @param blobStoreContext the BlobStoreContext to use
     * @param providerOrApi the provider or API to use for cloud storage
     */
    protected JCloudBitStoreService(BlobStoreContext blobStoreContext, String providerOrApi) {
        this.blobStoreContext = blobStoreContext;
        this.providerOrApi = providerOrApi;
    }

    /**
     * Sets whether to use relative paths for storing bitstreams.
     *
     * @param useRelativePath true to use relative paths, false otherwise
     */
    public void setUseRelativePath(boolean useRelativePath) {
        this.useRelativePath = useRelativePath;
    }

    /**
     * Sets whether the service is enabled.
     *
     * @param enabled true to enable the service, false otherwise
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Sets the container name for storing bitstreams.
     *
     * @param container the container name
     */
    public void setContainer(String container) {
        this.container = container;
    }

    /**
     * Gets the subfolder within the bucket where objects are stored.
     *
     * @return the subfolder name
     */
    public String getSubfolder() {
        return subfolder;
    }

    /**
     * Sets the subfolder within the bucket where objects are stored.
     *
     * @param subfolder the subfolder name
     */
    public void setSubfolder(String subfolder) {
        this.subfolder = subfolder;
    }

    /**
     * Sets the identity for cloud storage authentication.
     *
     * @param identity the identity
     */
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    /**
     * Sets the credentials for cloud storage authentication.
     *
     * @param credential the credentials
     */
    public void setCredentials(@Nullable String credential) {
        this.credential = credential;
    }

    /**
     * Sets the provider or API for cloud storage.
     *
     * @param providerOrApi the provider or API
     */
    public void setProviderOrApi(String providerOrApi) {
        this.providerOrApi = providerOrApi;
    }

    /**
     * Sets the endpoint for cloud storage.
     *
     * @param endpoint the endpoint
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Sets the maximum counter value for refreshing the context.
     *
     * @param maxCounter the maximum counter value
     */
    public void setMaxCounter(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    /**
     * Sets the properties for cloud storage configuration.
     *
     * @param overrides the properties
     */
    public void setOverrides(Properties overrides) {
        this.properties = overrides;
    }

    /**
     * Checks if the service is enabled.
     *
     * @return true if the service is enabled, false otherwise
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Initializes the cloud storage context.
     *
     * @throws IOException if an error occurs during initialization
     */
    @Override
    public void init() throws IOException {
        if (this.isInitialized()) {
            return;
        }
        try {
            this.builder = ContextBuilder.newBuilder(providerOrApi);
            if (StringUtils.isNotEmpty(endpoint)) {
                this.builder = this.builder.endpoint(endpoint);
            }
            if (properties != null && !properties.isEmpty()) {
                this.builder = this.builder.overrides(properties);
            }
            if (StringUtils.isNotEmpty(identity) && StringUtils.isNotEmpty(credential)) {
                this.builder = this.builder.credentials(identity, credential);
            }
            blobStoreContext = this.builder.buildView(BlobStoreContext.class);
            this.initialized = true;
        } catch (Exception e) {
            log.error(e.getMessage(),e);
            this.initialized = false;
        }
    }

    /**
     * Refreshes the cloud storage context if needed. Can be used to reset connection pool every
     * {@code @maxCounter} requests if thrid party service start to show connection pooling issues.
     * Defaults to never reset the connection pool.
     */
    private synchronized void refreshContextIfNeeded() {

        // do not reset context if maxCounter set to -1
        if (maxCounter < 0) {
            return;
        }

        counter++;
        // Close and Recreate connection between JClouds and remote service
        if (counter == maxCounter) {
            counter = 0;
            blobStoreContext.close();
            blobStoreContext = this.builder.buildView(BlobStoreContext.class);
        }
    }

    /**
     * Generates a unique identifier for a bitstream.
     *
     * @return the generated identifier
     */
    @Override
    public String generateId() {
        return Utils.generateKey();
    }

    /**
     * Retrieves a bitstream as an InputStream.
     *
     * @param bitstream the bitstream to retrieve
     * @return the InputStream of the bitstream
     * @throws IOException if an error occurs during retrieval
     */
    @Override
    public InputStream get(final Bitstream bitstream) throws IOException {
        final File file = getFile(bitstream);
        return get(file);
    }

    /**
     * Retrieves a file as an InputStream.
     *
     * @param file the file to retrieve
     * @return the InputStream of the file
     * @throws IOException if an error occurs during retrieval
     */
    private InputStream get(File file) throws IOException {
        BlobStore blobStore = blobStoreContext.getBlobStore();
        if (blobStore.blobExists(getContainer(), file.toString())) {
            Blob blob = blobStore.getBlob(getContainer(), file.toString());
            refreshContextIfNeeded();
            return blob.getPayload().openStream();
        }
        throw new IOException("File not found: " + file);
    }

    /**
     * Removes a bitstream from the cloud storage.
     *
     * @param bitstream the bitstream to remove
     * @throws IOException if an error occurs during removal
     */
    @Override
    public void remove(Bitstream bitstream) throws IOException {
        File file = getFile(bitstream);
        BlobStore blobStore = blobStoreContext.getBlobStore();
        blobStore.removeBlob(getContainer(), file.toString());
        deleteParents(file);
    }

    /**
     * Utility Method: Prefix the key with a subfolder, if this instance assets are stored within subfolder
     *
     * @param id DSpace bitstream internal ID
     * @return full key prefixed with a subfolder, if applicable
     */
    public String getFullKey(String id) {
        StringBuilder bufFilename = new StringBuilder();
        if (StringUtils.isNotEmpty(this.subfolder)) {
            bufFilename.append(this.subfolder);
            appendSeparator(bufFilename);
        }

        if (this.useRelativePath) {
            bufFilename.append(getRelativePath(id));
        } else {
            bufFilename.append(id);
        }

        if (log.isDebugEnabled()) {
            log.debug("Container filepath for " + id + " is "
                    + bufFilename.toString());
        }

        return bufFilename.toString();
    }

    /**
     * Computes the relative path for a bitstream.
     *
     * there are 2 cases:
     * - conventional bitstream, conventional storage
     * - registered bitstream, conventional storage
     *  conventional bitstream: dspace ingested, dspace random name/path
     *  registered bitstream: registered to dspace, any name/path
     *
     * @param sInternalId the internal ID of the bitstream
     * @return the computed relative path
     */
    private String getRelativePath(String sInternalId) {
        BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance()
                .getBitstreamStorageService();

        String sIntermediatePath = StringUtils.EMPTY;
        if (bitstreamStorageService.isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(2);
        } else {
            sInternalId = sanitizeIdentifier(sInternalId);
            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        return sIntermediatePath + sInternalId;
    }

    /**
     * Deletes parent directories if they are empty.
     *
     * @param file the file whose parent directories to delete
     */
    private void deleteParents(File file) {
        if (file == null) {
            return;
        }
        final BlobStore blobStore = blobStoreContext.getBlobStore();
        for (int i = 0; i < directoryLevels; i++) {
            final File directory = file.getParentFile();
            final ListContainerOptions options = new ListContainerOptions();
            options.inDirectory(directory.getPath());
            long blobs = blobStore.countBlobs(getContainer(), options);
            if (blobs != 0) {
                break;
            }
            blobStore.deleteDirectory(getContainer(), directory.getPath());
            file = directory;
        }
    }

    /**
     * Stores a byte source as a bitstream.
     *
     * @param byteSource the byte source to store
     * @param bitstream the bitstream to store
     * @throws IOException if an error occurs during storage
     */
    public void put(ByteSource byteSource, Bitstream bitstream)  throws IOException {

        String key = getFullKey(bitstream.getInternalId());

        /* set type to sane default */
        String type = MediaType.OCTET_STREAM.toString();

        /* attempt to get type if the source is a Bitstream */
        if (byteSource instanceof BitstreamByteSource) {
            type = getMIMEType(((BitstreamByteSource) byteSource).getBitstream());
        }

        BlobStore blobStore = blobStoreContext.getBlobStore();
        String container = getContainer();

        if (!blobStore.containerExists(container)) {
            blobStore.createContainerInLocation(null, container);
        }

        Blob blob = blobStore.blobBuilder(key)
                .payload(byteSource)
                .contentDisposition(key)
                .contentLength(byteSource.size())
                .contentType(type)
                .build();

        /* Utilize large file transfer to S3 via multipart post */
        blobStore.putBlob(container, blob, Builder.multipart());
    }

    /**
     * Stores a stream of bits.
     *
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param bitstream the bitstream to store
     * @param in the stream of bits to store
     * @throws IOException if a problem occurs while storing the bits
     */
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

            put(Files.asByteSource(scratchFile), bitstream);

            bitstream.setSizeBytes(contentLength);
            bitstream.setChecksum(localChecksum);
            bitstream.setChecksumAlgorithm(CSA);

        } catch (Exception e) {
            log.error("put(" + bitstream.getInternalId() + ", is)", e);
            throw new IOException(e);
        } finally {
            if (!scratchFile.delete()) {
                scratchFile.deleteOnExit();
            }
        }
    }

    /**
     * Gets the MIME type of a bitstream.
     *
     * @param bitstream the bitstream to get the MIME type for
     * @return the MIME type of the bitstream
     */
    public static String getMIMEType(final Bitstream bitstream) {
        try {
            BitstreamFormat format = bitstream.getFormat(new Context());
            return format == null ? null : format.getMIMEType();
        } catch (SQLException ignored) {
            throw new RuntimeException(ignored);
        }
    }

    /**
     * Retrieves metadata about a bitstream.
     *
     * @param bitstream the bitstream to retrieve metadata for
     * @param attrs the list of attributes to retrieve
     * @return a map of metadata attributes and their values
     * @throws IOException if an error occurs during retrieval
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> about(Bitstream bitstream, List<String> attrs) throws IOException {
        File file = getFile(bitstream);
        BlobStore blobStore = blobStoreContext.getBlobStore();
        BlobMetadata blobMetadata = blobStore.blobMetadata(getContainer(), file.toString());
        Map<String, Object> metadata = new HashMap<>();
        if (blobMetadata != null) {
            ContentMetadata contentMetadata = blobMetadata.getContentMetadata();

            if (contentMetadata != null) {
                metadata.put("size_bytes", String.valueOf(contentMetadata.getContentLength()));
                final HashCode hashCode = contentMetadata.getContentMD5AsHashCode();
                if (hashCode != null) {
                    metadata.put("checksum", Utils.toHex(contentMetadata.getContentMD5AsHashCode().asBytes()));
                    metadata.put("checksum_algorithm", CSA);
                }
                metadata.put("modified", String.valueOf(blobMetadata.getLastModified().getTime()));

                metadata.put("ContentDisposition", contentMetadata.getContentDisposition());
                metadata.put("ContentEncoding", contentMetadata.getContentEncoding());
                metadata.put("ContentLanguage", contentMetadata.getContentLanguage());
                metadata.put("ContentType", contentMetadata.getContentType());

                if (contentMetadata.getExpires() != null) {
                    metadata.put("Expires", contentMetadata.getExpires().getTime());
                }
            }
            return metadata;
        }
        return null;
    }

    /**
     * Gets the file corresponding to a bitstream.
     *
     * @param bitstream the bitstream to get the file for
     * @return the file corresponding to the bitstream
     * @throws IOException if an error occurs during retrieval
     */
    public File getFile(Bitstream bitstream) throws IOException {
        String id = bitstream.getInternalId();
        id = getFullKey(id);
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + bitstream.getInternalId() + " is " + id);
        }
        return new File(id);
    }

    /**
     * Gets the container name for storing bitstreams.
     *
     * @return the container name
     */
    private String getContainer() {
        return container;
    }
}
