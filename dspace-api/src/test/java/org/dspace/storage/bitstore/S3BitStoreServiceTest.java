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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Supplier;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;




/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 *
 */
public class S3BitStoreServiceTest extends AbstractUnitTest {

    private S3BitStoreService s3BitStoreService;

    @Mock
    private AmazonS3Client s3Service;

    @Mock
    private TransferManager tm;

    @Mock
    private Bitstream bitstream;

    @Before
    public void setUp() throws Exception {
        this.s3BitStoreService = new S3BitStoreService(s3Service, tm);
    }

    private Supplier<AmazonS3> mockedServiceSupplier() {
        return () -> this.s3Service;
    }

    @Test
    public void givenBucketWhenInitThenUsesSameBucket() throws IOException {
        String bucketName = "Bucket0";
        s3BitStoreService.setBucketName(bucketName);
        when(this.s3Service.doesBucketExist(bucketName)).thenReturn(false);

        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());

        this.s3BitStoreService.init();

        verify(this.s3Service).doesBucketExist(bucketName);
        verify(this.s3Service, Mockito.times(1)).createBucket(bucketName);
        assertThat(s3BitStoreService.getAwsAccessKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsSecretKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());
    }

    @Test
    public void givenEmptyBucketWhenInitThenUsesDefaultBucket() throws IOException {
        assertThat(s3BitStoreService.getBucketName(), isEmptyOrNullString());
        when(this.s3Service.doesBucketExist(startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX))).thenReturn(false);
        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());

        this.s3BitStoreService.init();

        verify(this.s3Service, Mockito.times(1)).createBucket(startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX));
        assertThat(s3BitStoreService.getBucketName(), Matchers.startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX));
        assertThat(s3BitStoreService.getAwsAccessKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsSecretKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());
    }

    @Test
    public void givenAccessKeysWhenInitThenVerifiesCorrectBuilderCreation() throws IOException {
        assertThat(s3BitStoreService.getAwsAccessKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsSecretKey(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getBucketName(), isEmptyOrNullString());
        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());
        when(this.s3Service.doesBucketExist(startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX))).thenReturn(false);

        final String awsAccessKey = "ACCESS_KEY";
        final String awsSecretKey = "SECRET_KEY";

        this.s3BitStoreService.setAwsAccessKey(awsAccessKey);
        this.s3BitStoreService.setAwsSecretKey(awsSecretKey);

        try (MockedStatic<S3BitStoreService> mockedS3BitStore = Mockito.mockStatic(S3BitStoreService.class)) {
            mockedS3BitStore
                .when(() ->
                    S3BitStoreService.amazonClientBuilderBy(
                            ArgumentMatchers.any(Regions.class),
                            ArgumentMatchers.argThat(
                                    credentials ->
                                        awsAccessKey.equals(credentials.getAWSAccessKeyId()) &&
                                        awsSecretKey.equals(credentials.getAWSSecretKey())
                            )
                     )
                 )
                .thenReturn(this.mockedServiceSupplier());

            this.s3BitStoreService.init();

            mockedS3BitStore.verify(
                    () ->
                    S3BitStoreService.amazonClientBuilderBy(
                            ArgumentMatchers.any(Regions.class),
                            ArgumentMatchers.argThat(
                                    credentials ->
                                        awsAccessKey.equals(credentials.getAWSAccessKeyId()) &&
                                        awsSecretKey.equals(credentials.getAWSSecretKey())
                            )
                    )
            );
        }


        verify(this.s3Service, Mockito.times(1)).createBucket(startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX));
        assertThat(s3BitStoreService.getBucketName(), Matchers.startsWith(S3BitStoreService.DEFAULT_BUCKET_PREFIX));
        assertThat(s3BitStoreService.getAwsAccessKey(), Matchers.equalTo(awsAccessKey));
        assertThat(s3BitStoreService.getAwsSecretKey(), Matchers.equalTo(awsSecretKey));
        assertThat(s3BitStoreService.getAwsRegionName(), isEmptyOrNullString());
    }

    @Test
    public void givenBucketBitStreamIdInputStreamWhenRetrievingFromS3ThenUsesBucketBitStreamId() throws IOException {
        String bucketName = "BucketTest";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        this.s3BitStoreService.init();

        Download download = mock(Download.class);

        when(tm.download(any(GetObjectRequest.class), any(File.class)))
            .thenAnswer(invocation -> writeIntoFile(download, invocation, "Test file content"));

        InputStream inputStream = this.s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, Charset.defaultCharset()), is("Test file content"));

    }

    @Test
    public void givenSubFolderWhenRequestsItemFromS3ThenTheIdentifierShouldHaveProperPath() throws IOException {
        String bucketName = "BucketTest";
        String bitStreamId = "012345";
        String subfolder = "/test/DSpace7/";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        this.s3BitStoreService.setSubfolder(subfolder);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        Download download = mock(Download.class);

        when(tm.download(any(GetObjectRequest.class), any(File.class)))
            .thenAnswer(invocation -> writeIntoFile(download, invocation, "Test file content"));

        this.s3BitStoreService.init();
        InputStream inputStream = this.s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, Charset.defaultCharset()), is("Test file content"));

    }

    @Test
    public void givenBitStreamIdentifierLongerThanPossibleWhenIntermediatePathIsComputedThenIsSplittedAndTruncated() {
        String path = "01234567890123456789";
        String computedPath = this.s3BitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "45" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenBitStreamIdentifierShorterThanAFolderLengthWhenIntermediatePathIsComputedThenIsSingleFolder() {
        String path = "0";
        String computedPath = this.s3BitStoreService.getIntermediatePath(path);
        String expectedPath = "0" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenPartialBitStreamIdentifierWhenIntermediatePathIsComputedThenIsCompletlySplitted() {
        String path = "01234";
        String computedPath = this.s3BitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "4" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenMaxLengthBitStreamIdentifierWhenIntermediatePathIsComputedThenIsSplittedAllAsSubfolder() {
        String path = "012345";
        String computedPath = this.s3BitStoreService.getIntermediatePath(path);
        String expectedPath = "01" + File.separator + "23" + File.separator + "45" + File.separator;
        assertThat(computedPath, equalTo(expectedPath));
    }

    @Test
    public void givenBitStreamIdentifierWhenIntermediatePathIsComputedThenNotEndingDoubleSlash() throws IOException {
        StringBuilder path = new StringBuilder("01");
        String computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        int slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("3");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("4");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));

        path.append("56789");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator + File.separator)));
    }

    @Test
    public void givenBitStreamIdentidierWhenIntermediatePathIsComputedThenMustBeSplitted() throws IOException {
        StringBuilder path = new StringBuilder("01");
        String computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        int slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("3");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("4");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("56789");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));
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
        String computedPath = this.s3BitStoreService.sanitizeIdentifier(sInternalId);
        assertThat(computedPath, Matchers.not(Matchers.startsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.containsString(File.separator)));
    }

    @Test
    public void givenBitStreamWhenRemoveThenCallS3DeleteMethod() throws Exception {
        String bucketName = "BucketTest";
        String bitStreamId = "BitStreamId";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        this.s3BitStoreService.init();
        this.s3BitStoreService.remove(bitstream);

        verify(this.s3Service, Mockito.times(1)).deleteObject(ArgumentMatchers.eq(bucketName),
                ArgumentMatchers.eq(bitStreamId));

    }

    @Test
    public void givenBitStreamWhenPutThenCallS3PutMethodAndStoresInBitStream() throws Exception {
        String bucketName = "BucketTest";
        String bitStreamId = "BitStreamId";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        InputStream in = IOUtils.toInputStream("Test file content", Charset.defaultCharset());

        Upload upload = Mockito.mock(Upload.class);
        UploadResult uploadResult = Mockito.mock(UploadResult.class);
        when(upload.waitForUploadResult()).thenReturn(uploadResult);

        when(this.tm.upload(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(upload);

        this.s3BitStoreService.init();
        this.s3BitStoreService.put(bitstream, in);

        verify(this.bitstream).setSizeBytes(17);
        verify(this.bitstream, times(2)).getInternalId();
        verify(this.bitstream).setChecksum("ac79653edeb65ab5563585f2d5f14fe9");
        verify(this.bitstream).setChecksumAlgorithm(org.dspace.storage.bitstore.S3BitStoreService.CSA);
        verify(this.tm).upload(eq(bucketName), eq(bitStreamId), any(File.class));

        verifyNoMoreInteractions(this.bitstream, this.tm);

    }

    private Download writeIntoFile(Download download, InvocationOnMock invocation, String content) {

        File file = invocation.getArgument(1, File.class);

        try {
            FileUtils.write(file, content, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return download;
    }

    private int computeSlashes(String internalId) {
        int minimum = internalId.length();
        int slashesPerLevel = minimum / S3BitStoreService.digitsPerLevel;
        int odd = Math.min(1, minimum % S3BitStoreService.digitsPerLevel);
        int slashes = slashesPerLevel + odd;
        return Math.min(slashes, S3BitStoreService.directoryLevels);
    }

}
