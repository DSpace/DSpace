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

import java.io.File;
import java.io.IOException;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Bitstream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author Nathan Buckingham
 *
 */
public class JCloudBitStoreServiceTest extends AbstractUnitTest {


    private JCloudBitStoreService jCloudBitStoreService;

    @Mock
    private Bitstream bitstream;

    @Before
    public void setUp() throws Exception {
        this.jCloudBitStoreService = new JCloudBitStoreService("filesystem");
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
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

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
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("2");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("3");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("4");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
        slashes = computeSlashes(path.toString());
        assertThat(computedPath, Matchers.endsWith(File.separator));
        assertThat(computedPath.split(File.separator).length, Matchers.equalTo(slashes));

        path.append("56789");
        computedPath = this.jCloudBitStoreService.getIntermediatePath(path.toString());
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

}
