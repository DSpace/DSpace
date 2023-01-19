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
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.commons.io.FileUtils;
import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.dspace.curate.Utils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;




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

    @Mock
    private Bitstream externalBitstream;

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
        String bitStreamId = "BitStreamId";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        S3Object object = Mockito.mock(S3Object.class);
        S3ObjectInputStream inputStream = Mockito.mock(S3ObjectInputStream.class);
        when(object.getObjectContent()).thenReturn(inputStream);
        when(this.s3Service.getObject(ArgumentMatchers.any(GetObjectRequest.class))).thenReturn(object);

        this.s3BitStoreService.init();
        assertThat(this.s3BitStoreService.get(bitstream), Matchers.equalTo(inputStream));

        verify(this.s3Service).getObject(
                ArgumentMatchers.argThat(
                    request ->
                    bucketName.contentEquals(request.getBucketName()) &&
                    bitStreamId.contentEquals(request.getKey())
                )
        );

    }

    @Test
    public void givenBucketBitStreamIdWhenNothingFoundOnS3ThenReturnsNull() throws IOException {
        String bucketName = "BucketTest";
        String bitStreamId = "BitStreamId";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        when(this.s3Service.getObject(ArgumentMatchers.any(GetObjectRequest.class))).thenReturn(null);

        this.s3BitStoreService.init();
        assertThat(this.s3BitStoreService.get(bitstream), Matchers.nullValue());

        verify(this.s3Service).getObject(
                ArgumentMatchers.argThat(
                        request ->
                        bucketName.contentEquals(request.getBucketName()) &&
                        bitStreamId.contentEquals(request.getKey())
                )
         );

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

        S3Object object = Mockito.mock(S3Object.class);
        S3ObjectInputStream inputStream = Mockito.mock(S3ObjectInputStream.class);
        when(object.getObjectContent()).thenReturn(inputStream);
        when(this.s3Service.getObject(ArgumentMatchers.any(GetObjectRequest.class))).thenReturn(object);

        this.s3BitStoreService.init();
        assertThat(this.s3BitStoreService.get(bitstream), Matchers.equalTo(inputStream));

        verify(this.s3Service).getObject(
                ArgumentMatchers.argThat(
                    request ->
                    bucketName.equals(request.getBucketName()) &&
                    request.getKey().startsWith(subfolder) &&
                    request.getKey().contains(bitStreamId) &&
                    !request.getKey().contains(File.separator + File.separator)
                )
        );

    }

    @Test
    public void handleRegisteredIdentifierPrefixInS3() {
        String trueBitStreamId = "012345";
        String registeredBitstreamId = s3BitStoreService.REGISTERED_FLAG + trueBitStreamId;
        // Should be detected as registered bitstream
        assertTrue(this.s3BitStoreService.isRegisteredBitstream(registeredBitstreamId));
    }

    @Test
    public void stripRegisteredBitstreamPrefixWhenCalculatingPath() {
        // Set paths and IDs
        String s3Path = "UNIQUE_S3_PATH/test/bitstream.pdf";
        String registeredBitstreamId = s3BitStoreService.REGISTERED_FLAG + s3Path;
        // Paths should be equal, since the getRelativePath method should strip the registered -R prefix
        String relativeRegisteredPath = this.s3BitStoreService.getRelativePath(registeredBitstreamId);
        assertEquals(s3Path, relativeRegisteredPath);
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

        File file = Mockito.mock(File.class);
        InputStream in = Mockito.mock(InputStream.class);
        PutObjectResult putObjectResult = Mockito.mock(PutObjectResult.class);
        Upload upload = Mockito.mock(Upload.class);
        UploadResult uploadResult = Mockito.mock(UploadResult.class);
        when(upload.waitForUploadResult()).thenReturn(uploadResult);
        String mockedTag = "1a7771d5fdd7bfdfc84033c70b1ba555";
        when(file.length()).thenReturn(8L);
        try (MockedStatic<File> fileMock = Mockito.mockStatic(File.class)) {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                try (MockedStatic<Utils> curateUtils = Mockito.mockStatic(Utils.class)) {
                    curateUtils.when(() -> Utils.checksum((File) ArgumentMatchers.any(), ArgumentMatchers.any()))
                            .thenReturn(mockedTag);

                fileMock
                    .when(() -> File.createTempFile(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenReturn(file);

                when(this.tm.upload(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
                        .thenReturn(upload);

                this.s3BitStoreService.init();
                this.s3BitStoreService.put(bitstream, in);
                }
            }

        }

        verify(this.bitstream, Mockito.times(1)).setSizeBytes(
                ArgumentMatchers.eq(8L)
         );

        verify(this.bitstream, Mockito.times(1)).setChecksum(
                ArgumentMatchers.eq(mockedTag)
         );

        verify(this.tm, Mockito.times(1)).upload(
            ArgumentMatchers.eq(bucketName),
            ArgumentMatchers.eq(bitStreamId),
            ArgumentMatchers.eq(file)
         );

        verify(file, Mockito.times(1)).delete();

    }

    @Test
    public void givenBitStreamWhenCallingPutFileCopyingThrowsIOExceptionPutThenFileIsRemovedAndStreamClosed()
            throws Exception {
        String bucketName = "BucketTest";
        String bitStreamId = "BitStreamId";
        this.s3BitStoreService.setBucketName(bucketName);
        this.s3BitStoreService.setUseRelativePath(false);
        when(bitstream.getInternalId()).thenReturn(bitStreamId);

        File file = Mockito.mock(File.class);
        InputStream in = Mockito.mock(InputStream.class);
        try (MockedStatic<File> fileMock = Mockito.mockStatic(File.class)) {
            try (MockedStatic<FileUtils> fileUtilsMock = Mockito.mockStatic(FileUtils.class)) {
                fileUtilsMock
                    .when(() -> FileUtils.copyInputStreamToFile(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenThrow(IOException.class);
                fileMock
                    .when(() -> File.createTempFile(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenReturn(file);

                this.s3BitStoreService.init();
                assertThrows(IOException.class, () -> this.s3BitStoreService.put(bitstream, in));
            }

        }

        verify(this.bitstream, Mockito.never()).setSizeBytes(ArgumentMatchers.any(Long.class));

        verify(this.bitstream, Mockito.never()).setChecksum(ArgumentMatchers.any(String.class));

        verify(this.s3Service, Mockito.never()).putObject(ArgumentMatchers.any(PutObjectRequest.class));

        verify(file, Mockito.times(1)).delete();

    }

    private int computeSlashes(String internalId) {
        int minimum = internalId.length();
        int slashesPerLevel = minimum / S3BitStoreService.digitsPerLevel;
        int odd = Math.min(1, minimum % S3BitStoreService.digitsPerLevel);
        int slashes = slashesPerLevel + odd;
        return Math.min(slashes, S3BitStoreService.directoryLevels);
    }

}
