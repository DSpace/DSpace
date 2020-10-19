/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link DocumentCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private ReferCrosswalkMapper referCrosswalkMapper;

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        this.referCrosswalkMapper = new DSpace().getSingletonService(ReferCrosswalkMapper.class);
        assertThat(referCrosswalkMapper, notNullValue());

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = buildItem();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, item, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasTheExpectedContent(out);
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithoutImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = buildItem();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, item, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasExpectedContent(out);
        }

    }

    @Test
    public void testPdfCrosswalkPersonDisseminateWithImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = buildItem();

        Bundle bundle = BundleBuilder.createBundle(context, item)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-pdf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, item, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatPdfHasTheExpectedContent(out);
        }

    }

    @Test
    public void testRtfCrosswalkPersonDisseminateWithImage() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = buildItem();

        Bundle bundle = BundleBuilder.createBundle(context, item)
            .withName("ORIGINAL")
            .build();

        BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-rtf");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            streamCrosswalkDefault.disseminate(context, item, out);
            assertThat(out.toString(), not(isEmptyString()));
            assertThatRtfHasExpectedContent(out);
        }

    }

    private Item buildItem() {
        Item item = createItem(context, collection)
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withVariantName("J.S.")
            .withVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("111-222-333")
            .withScopusAuthorIdentifier("444-555-666")
            .withPersonAffiliation("University")
            .withPersonAffiliationStartDate("2020-01-02")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Researcher")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2015-01-01")
            .withPersonAffiliationEndDate("2020-01-01")
            .withPersonAffiliationRole("Developer")
            .withDescriptionAbstract(getBiography())
            .withPersonCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .withPersonQualification("Second Qualification")
            .withPersonQualificationStartDate("2016-01-02")
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        return item;
    }

    private String getBiography() {
        return "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.Lorem ipsum dolor sit amet, consectetur "
            + "adipiscing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris "
            + "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit "
            + "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in "
            + "culpa qui officia deserunt mollit anim id est laborum.";
    }

    private void assertThatRtfHasExpectedContent(ByteArrayOutputStream out) throws IOException, BadLocationException {
        RTFEditorKit rtfParser = new RTFEditorKit();
        Document document = rtfParser.createDefaultDocument();
        rtfParser.read(new ByteArrayInputStream(out.toByteArray()), document, 0);
        String content = document.getText(0, document.getLength());
        assertThatHasExpectedContent(content);
    }

    private void assertThatPdfHasTheExpectedContent(ByteArrayOutputStream out)
        throws InvalidPasswordException, IOException {
        PDDocument document = PDDocument.load(out.toByteArray());
        String content = new PDFTextStripper().getText(document);
        assertThatHasExpectedContent(content);
    }

    private void assertThatHasExpectedContent(String content) {
        assertThat(content, containsString("John Smith"));
        assertThat(content, containsString("Researcher at University"));
        assertThat(content, containsString("Birth Date: 1992-06-26"));
        assertThat(content, containsString("Gender: M"));
        assertThat(content, containsString("Country: England"));
        assertThat(content, containsString("Email: test@test.com"));
        assertThat(content, containsString("ORCID: 0000-0002-9079-5932"));
        assertThat(content, containsString("Scopus Author IDs: 111-222-333, 444-555-666"));
        assertThat(content, containsString("Lorem ipsum dolor sit amet"));
        assertThat(content, containsString("Affiliations"));
        assertThat(content, containsString("Researcher at University from 2020-01-02"));
        assertThat(content, containsString("Developer at Company from 2015-01-01 to 2020-01-01"));
        assertThat(content, containsString("Education"));
        assertThat(content, containsString("Student at School from 2000-01-01 to 2005-01-01"));
        assertThat(content, containsString("First Qualification from 2015-01-01 to 2016-01-01"));
        assertThat(content, containsString("Second Qualification from 2016-01-02"));
        assertThat(content, containsString("Other informations"));
        assertThat(content, containsString("Working groups: First work group, Second work group"));
        assertThat(content, containsString("Interests: Science"));
        assertThat(content, containsString("Knows languages: English, Italian"));
        assertThat(content, containsString("Personal sites: www.test.com ( Test ) , www.john-smith.com , "
            + "www.site.com ( Site )"));
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
