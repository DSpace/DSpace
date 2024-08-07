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
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.utils.DSpace;
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
 * @author Mark Diggory, Nathan Buckingham
 */
public class JCloudBitStoreService extends BaseBitStoreService {

    private static final Logger log = LogManager.getLogger(JCloudBitStoreService.class);

    private Properties properties;
    private String providerOrApi;
    private ContextBuilder builder;
    private BlobStoreContext blobStoreContext;
    private String container;
    private String subFolder;
    private String identity;
    private String credential;
    private String endpoint;

    private boolean useRelativePath;
    private boolean enabled = false;
    private int counter = 0;
    private int maxCounter = 100;

    private static final String CSA = "MD5";

    public JCloudBitStoreService() {
    }

    public JCloudBitStoreService(String providerOrApi) {
        this.providerOrApi = providerOrApi;
    }

    public void setUseRelativePath(boolean useRelativePath) {
        this.useRelativePath = useRelativePath;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public void setCredentials(@Nullable String credential) {
        this.credential = credential;
    }

    public void setProviderOrApi(String providerOrApi) {
        this.providerOrApi = providerOrApi;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setMaxCounter(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    public void setOverrides(Properties overrides) {
        this.properties = overrides;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void init() throws IOException {
        if (this.isInitialized()) {
            return;
        }
        try {
            this.builder = ContextBuilder.newBuilder(providerOrApi);
            if (endpoint != null) {
                this.builder = this.builder.endpoint(endpoint);
            }
            blobStoreContext = this.builder.overrides(properties)
                    .credentials(identity, credential).buildView(BlobStoreContext.class);
            this.initialized = true;
        } catch (Exception e) {
            this.initialized = false;
        }
    }

    private synchronized void refreshContextIfNeeded() {
        counter++;
        // Destroying and recreating the connection between JClouds and CloudFiles
        if (counter == maxCounter) {
            counter = 0;
            blobStoreContext.close();
            blobStoreContext = this.builder.overrides(properties)
                    .credentials(identity, credential).buildView(BlobStoreContext.class);
        }
    }

    @Override
    public String generateId() {
        return Utils.generateKey();
    }

    @Override
    public InputStream get(final Bitstream bitstream) throws IOException {
        final File file = getFile(bitstream);
        return get(file);
    }

    private InputStream get(File file) throws IOException {
        BlobStore blobStore = blobStoreContext.getBlobStore();
        if (blobStore.blobExists(getContainer(), file.toString())) {
            Blob blob = blobStore.getBlob(getContainer(), file.toString());
            refreshContextIfNeeded();
            return blob.getPayload().openStream();
        }
        throw new IOException("File not found: " + file);
    }

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
        if (StringUtils.isNotEmpty(this.subFolder)) {
            bufFilename.append(this.subFolder);
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

    public void put(ByteSource byteSource, Bitstream bitstream)  throws IOException {

        final File file = getFile(bitstream);

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

        Blob blob = blobStore.blobBuilder(file.toString())
                .payload(byteSource)
                .contentDisposition(file.toString())
                .contentLength(byteSource.size())
                .contentType(type)
                .build();

        /* Utilize large file transfer to S3 via multipart post */
        blobStore.putBlob(container, blob, Builder.multipart());
    }

    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        File tmp = File.createTempFile("jclouds", "cache");
        try {
            // Inefficient caching strategy, however allows for use of JClouds store directly without CachingStore.
            // Make sure there is sufficient storage in temp directory.
            Files.asByteSink(tmp).writeFrom(in);
            in.close();
            put(Files.asByteSource(tmp), bitstream);
        } finally {
            if (!tmp.delete()) {
                tmp.deleteOnExit();
            }
        }
    }

    public static String getMIMEType(final Bitstream bitstream) {
        try {
            BitstreamFormat format = bitstream.getFormat(new Context());
            return format == null ? null : format.getMIMEType();
        } catch (SQLException ignored) {
            throw new RuntimeException(ignored);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map about(Bitstream bitstream, Map attrs) throws IOException {
        File file = getFile(bitstream);
        BlobStore blobStore = blobStoreContext.getBlobStore();
        BlobMetadata blobMetadata = blobStore.blobMetadata(getContainer(), file.toString());
        if (blobMetadata != null) {
            ContentMetadata contentMetadata = blobMetadata.getContentMetadata();

            if (contentMetadata != null) {
                attrs.put("size_bytes", String.valueOf(contentMetadata.getContentLength()));
                final HashCode hashCode = contentMetadata.getContentMD5AsHashCode();
                if (hashCode != null) {
                    attrs.put("checksum", Utils.toHex(contentMetadata.getContentMD5AsHashCode().asBytes()));
                    attrs.put("checksum_algorithm", CSA);
                }
                attrs.put("modified", String.valueOf(blobMetadata.getLastModified().getTime()));

                attrs.put("ContentDisposition", contentMetadata.getContentDisposition());
                attrs.put("ContentEncoding", contentMetadata.getContentEncoding());
                attrs.put("ContentLanguage", contentMetadata.getContentLanguage());
                attrs.put("ContentType", contentMetadata.getContentType());

                if (contentMetadata.getExpires() != null) {
                    attrs.put("Expires", contentMetadata.getExpires().getTime());
                }
            }
            return attrs;
        }
        return null;
    }

    public File getFile(Bitstream bitstream) throws IOException {
        String id = bitstream.getInternalId();
        id = getFullKey(id);
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + bitstream.getInternalId() + " is " + id);
        }
        return new File(id);
    }

    /**
     * Gets the URI of the content within the store.
     *
     * @param id the bitstream internal id.
     * @return the URI, which is a relative path to the content.
     */
    @SuppressWarnings("unused") // used by AVS2
    public URI getStoredURI(String id) {
        String tempID = getFullKey(id);
        if (log.isDebugEnabled()) {
            log.debug("Local URI for " + id + " is " + tempID);
        }
        return URI.create(id);
    }

    private String getContainer() {
        if (container == null) {
            container = new DSpace().getConfigurationService().getProperty("dspace.hostname");
        }
        return container;
    }
}
