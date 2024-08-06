/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.net.MediaType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.core.Utils;
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
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Map;

/**
 * JCloudBitstream asset store service
 *
 * @author Mark Diggory
 */
public class JCloudBitStoreService extends BaseBitStoreService {

    private static final Logger log = LogManager.getLogger(JCloudBitStoreService.class);

    private String proivderOrApi;
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
    private int maxCounter= 100;

    private static final String CSA = "MD5";

    @SuppressWarnings("WeakerAccess")
    public JCloudBitStoreService() {
    }

    @Autowired
    public void setUseRelativePath(boolean useRelativePath) {
        this.useRelativePath = useRelativePath;
    }

    @Autowired
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Autowired
    public void setContainer(String container) {
        this.container = container;
    }

    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    @Autowired
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    @Autowired
    public void setCredentials(@Nullable String credential) {
        this.credential = credential;
    }

    @Autowired
    public void setProivderOrApi(String proivderOrApi) {
        this.proivderOrApi = proivderOrApi;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setMaxCounter(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    @Override
    public void init() throws IOException {
        if (this.isInitialized()) {
            return;
        }
        this.builder = ContextBuilder.newBuilder(proivderOrApi);
        if (endpoint != null) {
            this.builder = this.builder.endpoint(endpoint);
        }
        blobStoreContext = this.builder.credentials(identity, credential).build(BlobStoreContext.class);
        this.initialized = true;
    }

    private synchronized void refreshContextIfNeeded() {
        counter++;
        // Destroying and recreating the connection between JClouds and CloudFiles
        if (counter == maxCounter) {
            counter = 0;
            blobStoreContext.close();
            blobStoreContext = this.builder.credentials(identity, credential).buildView(BlobStoreContext.class);
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
            if (blobs != 0){
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
        if(blobMetadata != null){
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
        StringBuilder sb = new StringBuilder();
        String id = bitstream.getInternalId();
        sb.append(getIntermediatePath(id));
        sb.append(id);
        if (log.isDebugEnabled())
        {
            log.debug("Local filename for " + id + " is " + sb.toString());
        }
        return new File(sb.toString());
    }

    /**
     * Gets the URI of the content within the store.
     *
     * @param id the bitstream internal id.
     * @return the URI, which is a relative path to the content.
     */
    @SuppressWarnings("unused") // used by AVS2
    public URI getStoredURI(String id) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIntermediatePath(id));
        sb.append(id);
        if (log.isDebugEnabled())
        {
            log.debug("Local URI for " + id + " is " + sb.toString());
        }
        return URI.create(sb.toString());
    }

    private String getContainer(){
        if(container == null){
            container = new DSpace().getConfigurationService().getProperty("dspace.hostname");
        }
        return container;
    }
}
