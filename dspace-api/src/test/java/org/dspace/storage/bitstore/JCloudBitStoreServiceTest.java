/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.ByteSource;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.hamcrest.Matchers;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.domain.BlobBuilder.PayloadBlobBuilder;
import org.jclouds.io.Payload;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * @author Nathan Buckingham
 *
 */
public class JCloudBitStoreServiceTest extends AbstractUnitTest {


    private JCloudBitStoreService jCloudBitStoreService;

    @Mock
    private BlobStoreContext blobStoreContext;

    @Mock
    private BlobStore blobStore;

    @Mock
    private Bitstream bitstream;

    @Before
    public void setUp() throws Exception {
        this.jCloudBitStoreService = new JCloudBitStoreService(blobStoreContext, "filesystem");
    }

    @Test
    public void getBitstreamTest() throws Exception {
        Blob blob = Mockito.mock(Blob.class);
        Payload payload = Mockito.mock(Payload.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        when(blob.getPayload()).thenReturn(payload);
        when(payload.openStream()).thenReturn(inputStream);
        when(blobStoreContext.getBlobStore()).thenReturn(blobStore);
        when(blobStore.getBlob(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(blob);
        when(blobStore.blobExists(ArgumentMatchers.any(), any())).thenReturn(true);
        assertThat(this.jCloudBitStoreService.get(bitstream), Matchers.equalTo(inputStream));
    }

    @Test
    public void removeBitstreamTest() throws Exception {
        String bitStreamId = "BitStreamId";
        when(bitstream.getInternalId()).thenReturn(bitStreamId);
        when(blobStoreContext.getBlobStore()).thenReturn(blobStore);
        try {
            this.jCloudBitStoreService.remove(bitstream);
        } catch (Exception e) {
            // will fail due to trying to remove files
        }
        verify(this.blobStore, Mockito.times(1)).removeBlob(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void replaceBitStreamTest() throws Exception {
        Blob blob = Mockito.mock(Blob.class);
        File file = Mockito.mock(File.class);
        BlobBuilder blobBuilder = Mockito.mock(BlobBuilder.class);
        PayloadBlobBuilder payloadBlobBuilder = Mockito.mock(PayloadBlobBuilder.class);

        when(blobStoreContext.getBlobStore()).thenReturn(blobStore);
        when(blobStore.blobBuilder(ArgumentMatchers.any())).thenReturn(blobBuilder);
        when(blobBuilder.payload(ArgumentMatchers.any(ByteSource.class))).thenReturn(payloadBlobBuilder);
        when(payloadBlobBuilder.contentDisposition(ArgumentMatchers.any())).thenReturn(payloadBlobBuilder);
        when(payloadBlobBuilder.contentLength(ArgumentMatchers.any(long.class))).thenReturn(payloadBlobBuilder);
        when(payloadBlobBuilder.contentType(ArgumentMatchers.any(String.class))).thenReturn(payloadBlobBuilder);
        when(payloadBlobBuilder.build()).thenReturn(blob);
        ByteSource byteSource = Mockito.mock(ByteSource.class);
        String mockedTag = "1a7771d5fdd7bfdfc84033c70b1ba555";
        this.jCloudBitStoreService.put(byteSource, bitstream);
        verify(blobStore, Mockito.times(1)).putBlob(ArgumentMatchers.any(),
                ArgumentMatchers.any(), ArgumentMatchers.any());
    }


    @Test
    public void givenBitStreamIdentifierLongerThanPossibleWhenIntermediatePathIsComputedThenIsSplittedAndTruncated() {
        String path = "01234567890123456789";
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "45" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }


    @Test
    public void givenBitStreamIdentifierShorterThanAFolderLengthWhenIntermediatePathIsComputedThenIsSingleFolder() {
        String path = "0";
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path);
        String expectedPath = "0" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenPartialBitStreamIdentifierWhenIntermediatePathIsComputedThenIsCompletlySplitted() {
        String path = "01234";
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "4" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenMaxLengthBitStreamIdentifierWhenIntermediatePathIsComputedThenIsSplittedAllAsSubfolder() {
        String path = "012345";
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "45" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenBitStreamIdentifierWhenIntermediatePathIsComputedThenNotEndingDoubleSlash() throws IOException {
        StringBuilder path = new StringBuilder("01");
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        int slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("3");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("4");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("56789");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));
    }

    @Test
    public void givenBitStreamIdentidierWhenIntermediatePathIsComputedThenMustBeSplitted() throws IOException {
        StringBuilder path = new StringBuilder("01");
        String computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        int slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("3");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("4");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("56789");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));
    }

    @Test
    public void givenBitStreamIdentifierWithSlashesWhenSanitizedThenSlashesMustBeRemoved() {
        String sInternalId = new StringBuilder("01")
                .append(File.separator)
                .append("22")
                .append(File.separator)
                .append("33")
                .append(File.separator)
                .append("4455")
                .toString();
        String computedPath = this.jCloudBitStoreService.sanitizeIdentifier(sInternalId);
        assertThat(computedPath, Matchers.not(Matchers.startsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.containsString(File.separator)));
    }

    private int computeSlashes(String internalId) {
        int minimum = internalId.length();
        int slashesPerLevel = minimum / S3BitStoreService.digitsPerLevel;
        int odd = Math.min(1, minimum % S3BitStoreService.digitsPerLevel);
        int slashes = slashesPerLevel + odd;
        return Math.min(slashes, S3BitStoreService.directoryLevels);
    }

    // Count the number of elements in a Unix or Windows path.
    // We use 'Paths' instead of splitting on slashes because these OSes use different path separators.
    private int countPathElements(String stringPath) {
        List<String> pathElements = new ArrayList<>();
        Paths.get(stringPath).forEach(p -> pathElements.add(p.toString()));
        return pathElements.size();
    }

}
