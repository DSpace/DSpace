/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CoverPageServiceTest extends AbstractDSpaceTest {

    @Mock
    Item item;

    List<MetadataValue> itemMetaData = new ArrayList<>();
    Set<String> metaDataKeys = new HashSet<>();

    CoverPageService sut;

    @Before
    public void setUp() throws Exception {
        Mockito.doReturn(itemMetaData).when(item).getMetadata();

        sut = new CoverPageService("dspace_coverpage", null);
    }

    @Test
    public void canRenderCoverPage() throws Exception {

        givenMetadataValues("dc_title", "My title");

        try (var coverPage = sut.renderCoverDocument(item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("My title"));
        }
    }

    @Test
    public void pageWithSubtitle() throws Exception {

        givenMetadataValues("dc_title", "My title");
        givenMetadataValues("dc_title_alternative", "subtitle");

        try (var coverPage = sut.renderCoverDocument(item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("My title: subtitle"));
        }
    }

    @Test
    public void multipleAuthors() throws Exception {

        givenMetadataValues("dc_title",
                "My title",
                "dc_contributor_author",
                "My author 1",
                "dc_contributor_author",
                "My author 2",
                "dc_creator",
                "My author 3");

        try (var coverPage = sut.renderCoverDocument(item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("My author 1; My author 2; My author 3"));
        }
    }

    @Test
    public void multipleEditors() throws Exception {

        givenMetadataValues("dc_title",
                "My title",
                "dc_contributor_author",
                "My author 1",
                "dc_contributor_editor",
                "My editor 1",
                "dc_contributor_editor",
                "My editor 2");

        try (var coverPage = sut.renderCoverDocument(item)) {
            assertThat(coverPage.getNumberOfPages(), equalTo(1));

            var text = new PDFTextStripper().getText(coverPage);

            assertThat(text, containsString("My editor 1; My editor 2"));
        }
    }

    private void givenMetadataValues(String... entries) {

        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < entries.length; i += 2) {
            int place = 0;
            if (metaDataKeys.contains(entries[i])) {
                place = 1;
            }

            itemMetaData.add(aMetadataValue(entries[i], entries[i + 1], place));
            metaDataKeys.add(entries[i]);
        }
    }

    private MetadataValue aMetadataValue(String key, String value, int place) {
        var mock = Mockito.mock(MetadataValue.class);

        var f = Mockito.mock(MetadataField.class);
        Mockito.doReturn(f).when(mock).getMetadataField();
        Mockito.doReturn(key).when(f).toString();

        Mockito.doReturn(value).when(mock).getValue();

        Mockito.doReturn(place).when(mock).getPlace();

        return mock;
    }
}