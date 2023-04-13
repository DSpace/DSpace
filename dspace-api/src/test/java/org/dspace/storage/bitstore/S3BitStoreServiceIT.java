/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static com.amazonaws.regions.Regions.DEFAULT_REGION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dspace.storage.bitstore.S3BitStoreService.CSA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import io.findify.s3mock.S3Mock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * @author Luca Giamminonni (luca.giamminonni at 4science.com)
 */
public class S3BitStoreServiceIT extends AbstractIntegrationTestWithDatabase {

    private static final String DEFAULT_BUCKET_NAME = "dspace-asset-localhost";

    private S3BitStoreService s3BitStoreService;

    private AmazonS3 amazonS3Client;

    private S3Mock s3Mock;

    private Collection collection;

    private File s3Directory;

    @Before
    public void setup() throws Exception {

        s3Directory = new File(System.getProperty("java.io.tmpdir"), "s3");

        s3Mock = S3Mock.create(8001, s3Directory.getAbsolutePath());
        s3Mock.start();

        amazonS3Client = createAmazonS3Client();

        s3BitStoreService = new S3BitStoreService(amazonS3Client);

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .build();

        context.restoreAuthSystemState();
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(s3Directory);
        s3Mock.shutdown();
    }

    @Test
    public void testBitstreamPutAndGetWithAlreadyPresentBucket() throws IOException {

        String bucketName = "testbucket";

        amazonS3Client.createBucket(bucketName);

        s3BitStoreService.setBucketName(bucketName);
        s3BitStoreService.init();

        assertThat(amazonS3Client.listBuckets(), contains(bucketNamed(bucketName)));

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        String expectedChecksum = Utils.toHex(generateChecksum(content));

        assertThat(bitstream.getSizeBytes(), is((long) content.length()));
        assertThat(bitstream.getChecksum(), is(expectedChecksum));
        assertThat(bitstream.getChecksumAlgorithm(), is(CSA));

        InputStream inputStream = s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, UTF_8), is(content));

        String key = s3BitStoreService.getFullKey(bitstream.getInternalId());
        ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(bucketName, key);
        assertThat(objectMetadata.getContentMD5(), is(expectedChecksum));

    }

    @Test
    public void testBitstreamPutAndGetWithoutSpecifingBucket() throws IOException {

        s3BitStoreService.init();

        assertThat(s3BitStoreService.getBucketName(), is(DEFAULT_BUCKET_NAME));

        assertThat(amazonS3Client.listBuckets(), contains(bucketNamed(DEFAULT_BUCKET_NAME)));

        context.turnOffAuthorisationSystem();
        String content = "Test bitstream content";
        Bitstream bitstream = createBitstream(content);
        context.restoreAuthSystemState();

        s3BitStoreService.put(bitstream, toInputStream(content));

        String expectedChecksum = Utils.toHex(generateChecksum(content));

        assertThat(bitstream.getSizeBytes(), is((long) content.length()));
        assertThat(bitstream.getChecksum(), is(expectedChecksum));
        assertThat(bitstream.getChecksumAlgorithm(), is(CSA));

        InputStream inputStream = s3BitStoreService.get(bitstream);
        assertThat(IOUtils.toString(inputStream, UTF_8), is(content));

        String key = s3BitStoreService.getFullKey(bitstream.getInternalId());
        ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(DEFAULT_BUCKET_NAME, key);
        assertThat(objectMetadata.getContentMD5(), is(expectedChecksum));

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

        ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(DEFAULT_BUCKET_NAME, key);
        assertThat(objectMetadata, notNullValue());

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
        assertThat(exception.getCause(), instanceOf(AmazonS3Exception.class));
        assertThat(((AmazonS3Exception) exception.getCause()).getStatusCode(), is(404));

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

        String expectedChecksum = Utils.toHex(generateChecksum(content));

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

    private byte[] generateChecksum(String content) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(content.getBytes());
            return m.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private AmazonS3 createAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
            .withEndpointConfiguration(new EndpointConfiguration("http://127.0.0.1:8001", DEFAULT_REGION.getName()))
            .build();
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
        return LambdaMatcher.matches(bucket -> bucket.getName().equals(name));
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

}
