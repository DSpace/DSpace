/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dspace.storage.bitstore.S3BitStoreService.CSA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.matcher.LambdaMatcher;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * @author Luca Giamminonni (luca.giamminonni at 4science.com)
 */
public class S3BitStoreServiceIT extends AbstractIntegrationTestWithDatabase {
    private static  S3MockContainer s3Mock = new S3MockContainer("4.8.0");

    private static S3AsyncClient s3AsyncClient;

    private static final String DEFAULT_BUCKET_NAME = "dspace-asset-localhost";

    private S3BitStoreService s3BitStoreService;

    private Collection collection;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @BeforeClass
    public static void setupS3() {
        s3Mock.start();

        s3AsyncClient = S3AsyncClient.crtBuilder()
                .endpointOverride(URI.create("http://127.0.0.1:" + s3Mock.getHttpServerPort()))
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();
    }

    @AfterClass
    public static void cleanupS3() {
        s3Mock.close();
        s3AsyncClient.close();
    }

    @Before
    public void setup() throws Exception {
        configurationService.setProperty("assetstore.s3.enabled", "true");

        s3BitStoreService = new S3BitStoreService(s3AsyncClient);
        s3BitStoreService.setEnabled(BooleanUtils.toBoolean(
                configurationService.getProperty("assetstore.s3.enabled")));
        s3BitStoreService.setS3ChecksumAlgorithm(ChecksumAlgorithm.SHA256);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void testBitstreamPutAndGetWithAlreadyPresentBucket() throws IOException {

        String bucketName = "testbucket";

        s3AsyncClient.createBucket(r -> r.bucket(bucketName)).join();

        s3BitStoreService.setBucketName(bucketName);
        s3BitStoreService.init();

        assertThat(s3AsyncClient.listBuckets().join().buckets(), hasItem(bucketNamed(bucketName)));

        context.turnOffAuthorisationSystem();
        String content                = "Test bitstream content";
        String contentOverOneSpan     = "This content span two chunks";
        String contentExactlyTwoSpans = "Test bitstream contentTest bitstream content";
        String contentOverOneTwoSpans = "Test bitstream contentThis content span three chunks";
        Bitstream bitstream = createBitstream(content);
        Bitstream bitstreamOverOneSpan = createBitstream(contentOverOneSpan);
        Bitstream bitstreamExactlyTwoSpans = createBitstream(contentExactlyTwoSpans);
        Bitstream bitstreamOverOneTwoSpans = createBitstream(contentOverOneTwoSpans);
        context.restoreAuthSystemState();

        checkGetPut(bucketName, content, bitstream);
        checkGetPut(bucketName, contentOverOneSpan, bitstreamOverOneSpan);
        checkGetPut(bucketName, contentExactlyTwoSpans, bitstreamExactlyTwoSpans);
        checkGetPut(bucketName, contentOverOneTwoSpans, bitstreamOverOneTwoSpans);

    }

    private void checkGetPut(String bucketName, String content, Bitstream bitstream) throws IOException {
        s3BitStoreService.put(bitstream, toInputStream(content));
        String expectedChecksum = Utils.toHex(generateChecksum("MD5", content));

        assertThat(bitstream.getSizeBytes(), is((long) content.length()));
        assertThat(bitstream.getChecksum(), is(expectedChecksum));
        assertThat(bitstream.getChecksumAlgorithm(), is(CSA));

        InputStream inputStream = s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, UTF_8), is(content));
    }

    @Test
    public void testBitstreamPutAndGetWithoutSpecifyingBucket() throws IOException {

        s3BitStoreService.init();

        assertThat(s3BitStoreService.getBucketName(), is(DEFAULT_BUCKET_NAME));

        assertThat(s3AsyncClient.listBuckets().join().buckets(), hasItem(bucketNamed(DEFAULT_BUCKET_NAME)));

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        String expectedChecksum = Utils.toHex(generateChecksum("MD5", content));

        assertThat(bitstream.getSizeBytes(), is((long) content.length()));
        assertThat(bitstream.getChecksum(), is(expectedChecksum));
        assertThat(bitstream.getChecksumAlgorithm(), is(CSA));

        InputStream inputStream = s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, UTF_8), is(content));
    }

    @Test
    public void testBitstreamPutAndGetWithSubFolder() throws IOException {

        s3BitStoreService.setSubfolder("test/DSpace7/");
        s3BitStoreService.init();

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        InputStream inputStream = s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, UTF_8), is(content));

        String key = s3BitStoreService.getFullKey(bitstream.getInternalId());
        assertThat(key, startsWith("test/DSpace7/"));

        HeadObjectResponse response = s3AsyncClient.headObject(r ->
            r.bucket(DEFAULT_BUCKET_NAME).key(key)).join();
        assertThat(response, notNullValue());
    }

    @Test
    public void testBitstreamDeletion() throws IOException {

        s3BitStoreService.init();

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        assertThat(s3BitStoreService.get(bitstream), notNullValue());

        s3BitStoreService.remove(bitstream);

        IOException exception = assertThrows(IOException.class, () -> s3BitStoreService.get(bitstream));
        assertThat(exception.getCause(), instanceOf(AwsServiceException.class));
        assertThat(((AwsServiceException) exception.getCause()).statusCode(), is(404));

    }

    @Test
    public void testAbout() throws IOException {

        s3BitStoreService.init();

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        Map<String, Object> about = s3BitStoreService.about(bitstream, List.of());
        assertThat(about.size(), is(0));

        about = s3BitStoreService.about(bitstream, List.of("size_bytes"));
        assertThat(about, hasEntry("size_bytes", 22L));
        assertThat(about.size(), is(1));

        about = s3BitStoreService.about(bitstream, List.of("size_bytes", "modified"));
        assertThat(about, hasEntry("size_bytes", 22L));
        assertThat(about, hasEntry(is("modified"), notNullValue()));
        assertThat(about.size(), is(2));

        String expectedChecksum = Utils.toHex(generateChecksum("MD5", content));

        about = s3BitStoreService.about(bitstream, List.of("size_bytes", "modified", "checksum"));
        assertThat(about, hasEntry("size_bytes", 22L));
        assertThat(about, hasEntry(is("modified"), notNullValue()));
        assertThat(about, hasEntry("checksum", expectedChecksum));
        assertThat(about.size(), is(3));

        about = s3BitStoreService.about(bitstream, List.of("size_bytes", "modified", "checksum", "checksum_algorithm"));
        assertThat(about, hasEntry("size_bytes", 22L));
        assertThat(about, hasEntry(is("modified"), notNullValue()));
        assertThat(about, hasEntry("checksum", expectedChecksum));
        assertThat(about, hasEntry("checksum_algorithm", CSA));
        assertThat(about.size(), is(4));

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
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

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
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("3");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("4");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(countPathElements(computedPath), Matchers.equalTo(slashes));

        path.append("56789");
        computedPath = this.s3BitStoreService.getIntermediatePath(path.toString());
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
        String computedPath = this.s3BitStoreService.sanitizeIdentifier(sInternalId);
        assertThat(computedPath, Matchers.not(Matchers.startsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.endsWith(File.separator)));
        assertThat(computedPath, Matchers.not(Matchers.containsString(File.separator)));
    }

    @Test
    public void testDoNotInitializeConfigured() throws Exception {
        String assetstores3enabledOldValue = configurationService.getProperty("assetstore.s3.enabled");
        configurationService.setProperty("assetstore.s3.enabled", "false");
        s3BitStoreService = new S3BitStoreService(s3AsyncClient);
        s3BitStoreService.init();
        assertFalse(s3BitStoreService.isInitialized());
        assertFalse(s3BitStoreService.isEnabled());
        configurationService.setProperty("assetstore.s3.enabled", assetstores3enabledOldValue);
    }

    private byte[] generateChecksum(String algorithm, String content) {
        try {
            MessageDigest m = MessageDigest.getInstance(algorithm);
            m.update(content.getBytes());
            return m.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Item createItem() {
        return ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();
    }

    private Bitstream createBitstream(String content) {
        try {
            return BitstreamBuilder
                .createBitstream(context, createItem(), toInputStream(content))
                .build();
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Matcher<? super Bucket> bucketNamed(String name) {
        return LambdaMatcher.matches(bucket -> bucket.name().equals(name));
    }

    private InputStream toInputStream(String content) {
        return IOUtils.toInputStream(content, UTF_8);
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
