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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.utils.DSpace;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link ReferCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private ItemService itemService;

    private MetadataFieldService mfss;

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private VirtualFieldMapper virtualFieldMapper;

    private Community community;

    private Collection collection;

    private VirtualField virtualFieldId;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        this.virtualFieldMapper = new DSpace().getSingletonService(VirtualFieldMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        this.itemService = new DSpace().getSingletonService(ItemServiceImpl.class);
        this.mfss = new DSpace().getSingletonService(MetadataFieldServiceImpl.class);

        this.virtualFieldId = this.virtualFieldMapper.getVirtualField("id");

        VirtualField mockedVirtualFieldId = mock(VirtualField.class);
        when(mockedVirtualFieldId.getMetadata(any(), any(), any())).thenReturn(new String[] { "mock-id" });
        this.virtualFieldMapper.setVirtualField("id", mockedVirtualFieldId);

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @After
    public void after() {
        this.virtualFieldMapper.setVirtualField("id", virtualFieldId);
    }

    @Test
    public void testPersonXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
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
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withDescriptionAbstract("Biography \n\t<This is my biography>")
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

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonXmlCerifDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Smith, John")
            .withVariantName("J.S.")
            .withVariantName("Smith John")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("SA-01")
            .withPersonEmail("test@test.com")
            .withResearcherIdentifier("R-01")
            .withResearcherIdentifier("R-02")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliation("Another Company")
            .withPersonAffiliationStartDate("2017-01-01")
            .withPersonAffiliationEndDate("2017-12-31")
            .withPersonAffiliationRole("Developer")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person-cerif.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPersonsXmlCerifDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item firstPerson = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Smith, John")
            .withVariantName("J.S.")
            .withVariantName("Smith John")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("SA-01")
            .withPersonEmail("test@test.com")
            .withResearcherIdentifier("R-01")
            .withResearcherIdentifier("R-02")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliation("Another Company")
            .withPersonAffiliationStartDate("2017-01-01")
            .withPersonAffiliationEndDate("2017-12-31")
            .withPersonAffiliationRole("Developer")
            .build();

        Item secondPerson = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("White, Walter")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5938")
            .withPersonEmail("w.w@test.com")
            .withResearcherIdentifier("R-03")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPerson, secondPerson).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons-cerif.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonWithEmptyGroupsXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
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
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("111-222-333")
            .withResearcherIdentifier("0001")
            .withResearcherIdentifier("0002")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withPersonCountry("England")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("person-with-empty-groups.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonXmlDisseminateWithPersonalPicture() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
            .withName("ORIGINAL")
            .build();

        Bitstream bitstream = BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalk = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCrosswalk.disseminate(context, item, out);

        assertThat(out.toString(), containsString("<personal-picture>" + bitstream.getID() + "</personal-picture>"));
    }

    @Test
    public void testPersonJsonDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withVernacularName("JOHN SMITH")
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
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withDescriptionAbstract("Biography: \n\t\"This is my biography\"")
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

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }
    }

    @Test
    public void testManyPersonsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Item firstItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        Item secondItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Adam White")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        // with multiple persons export the publications should not be exported
        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", firstItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstItem, secondItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPersonsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Item firstItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        Item secondItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Adam White")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        // with multiple persons export the publications should not be exported
        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", firstItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstItem, secondItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }
    }

    @Test
    public void testPublicationXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-04-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation("Company")
            .withEditor("Editor", "25887329-a648-46f9-a2ac-99319b8e9766")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("publication.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPublicationXmlDisseminateWithAuthorityOnFunder() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-04-01")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Test Funder")
            .withAcronym("TFO")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder", orgUnit.getID().toString())
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book::book part")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("publication-with-authority-on-funder.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPublicationXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstPublication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::learning object")
            .withIssueDate("2019-12-31")
            .withAuthor("Edward Smith", "8556d5b5-14e5-4009-9539-1ef9686d684d")
            .withAuthorAffiliation("Company")
            .withAuthor("Walter White", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Contract")
            .withFunder("Test Funder")
            .withAcronym("TF-01")
            .build();

        Item secondPublication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDoiIdentifier("doi:222.222/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::clinical trial")
            .withIssueDate("2010-02-01")
            .withAuthor("Jessie Pinkman")
            .withRelationFunding("Test Funding", funding.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPublication, secondPublication).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPublicationEndnoteDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withHandle("123456789/xxx")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthor("Walter White")
            .withAuthor("Jessie Pinkman")
            .withIsPartOf("Journal")
            .withPublisher("Publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withDescriptionAbstract("This is a publication to test export")
            .withSubject("test")
            .withSubject("publication")
            .withCitationStartPage("2")
            .withCitationEndPage("20")
            .withLanguage("en")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("endnote");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("endnote")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testProjectXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, project, out);

        try (FileInputStream fis = getFileInputStream("project.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testProjectJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
            .withProjectPartner("Partner OrgUnit")
            .withProjectPartner("Another Partner OrgUnit")
            .withProjectOrganization("First Member OrgUnit")
            .withProjectOrganization("Second Member OrgUnit")
            .withProjectOrganization("Third Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test funding")
            .withType("Award")
            .withFunder("Another OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, project, out);

        try (FileInputStream fis = getFileInputStream("project.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyProjectsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("First Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        Item secondProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("STP")
            .withTitle("Second Test Project")
            .withOpenaireId("55-66-77")
            .withOpenaireId("11-33-22")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2010-01-01")
            .withProjectEndDate("2012-12-31")
            .withProjectStatus("Status")
            .withProjectCoordinator("Second Coordinator OrgUnit")
            .withProjectInvestigator("Second investigator")
            .withProjectCoinvestigators("Coinvestigator")
            .withRelationEquipment("Another test equipment")
            .withOAMandateURL("oamandate")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstProject, secondProject).iterator(), out);

        try (FileInputStream fis = getFileInputStream("projects.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }

    }

    @Test
    public void testManyProjectsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("First Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        Item secondProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("STP")
            .withTitle("Second Test Project")
            .withOpenaireId("55-66-77")
            .withOpenaireId("11-33-22")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2010-01-01")
            .withProjectEndDate("2012-12-31")
            .withProjectStatus("Status")
            .withProjectCoordinator("Second Coordinator OrgUnit")
            .withProjectInvestigator("Second investigator")
            .withProjectCoinvestigators("Coinvestigator")
            .withRelationEquipment("Another test equipment")
            .withOAMandateURL("oamandate")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstProject, secondProject).iterator(), out);

        try (FileInputStream fis = getFileInputStream("projects.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }

    }

    @Test
    public void testOrgUnitXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, orgUnit, out);

        try (FileInputStream fis = getFileInputStream("orgUnit.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testOrgUnitJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, orgUnit, out);

        try (FileInputStream fis = getFileInputStream("orgUnit.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyOrgUnitsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        Item secondOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstOrgUnit, secondOrgUnit).iterator(), out);

        try (FileInputStream fis = getFileInputStream("orgUnits.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyOrgUnitsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        Item secondOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstOrgUnit, secondOrgUnit).iterator(), out);

        try (FileInputStream fis = getFileInputStream("orgUnits.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testEquipmentJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item equipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("T-EQ")
            .withTitle("Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("equipment-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, equipment, out);

        try (FileInputStream fis = getFileInputStream("equipment.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testEquipmentXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item equipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("T-EQ")
            .withTitle("Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("equipment-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, equipment, out);

        try (FileInputStream fis = getFileInputStream("equipment.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyEquipmentJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstEquipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("FT-EQ")
            .withTitle("First Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .build();

        Item secondEquipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("ST-EQ")
            .withTitle("Second Test Equipment")
            .withInternalId("ID-02")
            .withDescription("This is another equipment to test the export functionality")
            .withEquipmentOwnerPerson("John Smith")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("equipment-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstEquipment, secondEquipment).iterator(), out);

        try (FileInputStream fis = getFileInputStream("equipments.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyEquipmentXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstEquipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("FT-EQ")
            .withTitle("First Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .build();

        Item secondEquipment = ItemBuilder.createItem(context, collection)
            .withEntityType("Equipment")
            .withAcronym("ST-EQ")
            .withTitle("Second Test Equipment")
            .withInternalId("ID-02")
            .withDescription("This is another equipment to test the export functionality")
            .withEquipmentOwnerPerson("John Smith")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("equipment-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstEquipment, secondEquipment).iterator(), out);

        try (FileInputStream fis = getFileInputStream("equipments.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testFundingXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, funding, out);

        try (FileInputStream fis = getFileInputStream("funding.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testFundingJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, funding, out);

        try (FileInputStream fis = getFileInputStream("funding.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyFundingsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        Item secondFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("AT-FU")
            .withTitle("Another Test Funding")
            .withType("Grant")
            .withInternalId("ID-02")
            .withFundingIdentifier("0002")
            .withAmount("10.000,00")
            .withFunder("Test Funder")
            .withFundingStartDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstFunding, secondFunding).iterator(), out);

        try (FileInputStream fis = getFileInputStream("fundings.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyFundingsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        Item secondFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("AT-FU")
            .withTitle("Another Test Funding")
            .withType("Grant")
            .withInternalId("ID-02")
            .withFundingIdentifier("0002")
            .withAmount("10.000,00")
            .withFunder("Test Funder")
            .withFundingStartDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstFunding, secondFunding).iterator(), out);

        try (FileInputStream fis = getFileInputStream("fundings.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testPatentCerifXmlDisseminate() throws Exception {

        Item patent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Test patent")
            .withIssueDate("2021-01-01")
            .withPublisher("First publisher")
            .withPublisher("Second publisher")
            .withPatentNo("12345-666")
            .withAuthor("Walter White", "b6ff8101-05ec-49c5-bd12-cba7894012b7")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("John Smith", "will be referenced::ORCID::0000-0000-0012-3456")
            .withAuthorAffiliation("4Science")
            .withRightsHolder("Test Organization")
            .withDescriptionAbstract("This is a patent")
            .withRelationPatent("Another patent")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("patent-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, patent, out);

        try (FileInputStream fis = getFileInputStream("patent.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testManyPatentsCerifXmlDisseminate() throws Exception {

        Item firstPatent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Test patent")
            .withIssueDate("2021-01-01")
            .withPublisher("Publisher")
            .withPatentNo("12345-666")
            .build();

        Item secondPatent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Second patent")
            .withIssueDate("2011-01-01")
            .withPublisher("First publisher")
            .withPublisher("Second publisher")
            .withPatentNo("12345-777")
            .withAuthor("Walter White")
            .withAuthorAffiliation("4Science")
            .withRelationPatent("Another patent")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("patent-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPatent, secondPatent).iterator(), out);

        try (FileInputStream fis = getFileInputStream("patents.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testPatentJsonDisseminate() throws Exception {

        Item patent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Test patent")
            .withDateAccepted("2020-01-01")
            .withIssueDate("2021-01-01")
            .withLanguage("en")
            .withType("patent")
            .withPublisher("First publisher")
            .withPublisher("Second publisher")
            .withPatentNo("12345-666")
            .withAuthor("Walter White", "b6ff8101-05ec-49c5-bd12-cba7894012b7")
            .withAuthorAffiliation("4Science")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("John Smith", "will be referenced::ORCID::0000-0000-0012-3456")
            .withAuthorAffiliation("4Science")
            .withRightsHolder("Test Organization")
            .withDescriptionAbstract("This is a patent")
            .withRelationPatent("Another patent")
            .withSubject("patent")
            .withSubject("test")
            .withRelationFunding("Test funding")
            .withRelationProject("First project")
            .withRelationProject("Second project")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("patent-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, patent, out);

        try (FileInputStream fis = getFileInputStream("patent.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testManyPatentsJsonDisseminate() throws Exception {

        Item firstPatent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Test patent")
            .withIssueDate("2021-01-01")
            .withPublisher("Publisher")
            .withPatentNo("12345-666")
            .withSubject("subject")
            .withRelationProject("Project")
            .build();

        Item secondPatent = ItemBuilder.createItem(context, collection)
            .withEntityType("Patent")
            .withTitle("Second patent")
            .withIssueDate("2011-01-01")
            .withPublisher("First publisher")
            .withPublisher("Second publisher")
            .withPatentNo("12345-777")
            .withAuthor("Walter White")
            .withAuthorAffiliation("4Science")
            .withRelationPatent("Another patent")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("patent-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPatent, secondPatent).iterator(), out);

        try (FileInputStream fis = getFileInputStream("patents.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testDataSetCerifXmlDisseminate() throws Exception {

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withAcronym("T-PRJ")
            .build();

        Item dataSet = ItemBuilder.createItem(context, collection)
            .withEntityType("Product")
            .withTitle("Test DataSet")
            .withLanguage("EN")
            .withDescriptionVersion("V-01")
            .withDoiIdentifier("10.11234.12")
            .withAuthor("Walter White")
            .withAuthorAffiliation(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Jesse Pinkman", "94f05c08-6273-4a9e-b6cd-002fd8669fa0")
            .withAuthorAffiliation("4Science")
            .withPublisher("Publisher")
            .withDescriptionAbstract("This is a DataSet")
            .withSubject("DataSet")
            .withSubject("Keyword")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationEquipment("Test Equipment")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("product-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, dataSet, out);

        try (FileInputStream fis = getFileInputStream("product.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testManyDataSetsCerifXmlDisseminate() throws Exception {

        Item firstDataSet = ItemBuilder.createItem(context, collection)
            .withEntityType("Product")
            .withTitle("First DataSet")
            .withAuthor("Walter White")
            .withAuthorAffiliation(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPublisher("Publisher")
            .withSubject("DataSet")
            .withRelationProject("Test Project")
            .withRelationEquipment("First Equipment")
            .withRelationEquipment("Second Equipment")
            .build();

        Item secondDataSet = ItemBuilder.createItem(context, collection)
            .withEntityType("Product")
            .withTitle("Second DataSet")
            .withPublisher("Publisher")
            .withSubject("DataSet")
            .withRelationProject("First Project")
            .withRelationProject("Second Project")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("product-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstDataSet, secondDataSet).iterator(), out);

        try (FileInputStream fis = getFileInputStream("products.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testEventCerifXmlDisseminate() throws Exception {

        Item event = ItemBuilder.createItem(context, collection)
            .withEntityType("Event")
            .withTitle("Test Event")
            .withType("Conference")
            .withAcronym("TE")
            .withEventPlace("Milan")
            .withEventCountry("Italy")
            .withEventStartDate("2020-01-01")
            .withEventEndDate("2020-01-05")
            .withDescriptionAbstract("This is a test event")
            .withSubject("test")
            .withSubject("event")
            .withEventOrgUnitOrganizer("First Organizer")
            .withEventOrgUnitOrganizer("Second Organizer")
            .withEventProjectOrganizer("Third Organizer")
            .withEventOrgUnitSponsor("First Sponsor")
            .withEventProjectSponsor("Second Sponsor")
            .withEventOrgUnitPartner("First Partner")
            .withEventProjectPartner("Second Partner")
            .withEventProjectPartner("Third Partner")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("event-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, event, out);

        try (FileInputStream fis = getFileInputStream("event.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testManyEventsCerifXmlDisseminate() throws Exception {

        Item firstEvent = ItemBuilder.createItem(context, collection)
            .withEntityType("Event")
            .withTitle("First Event")
            .withType("Conference")
            .withAcronym("FE")
            .withEventPlace("Milan")
            .withEventCountry("Italy")
            .withEventStartDate("2020-01-01")
            .withEventEndDate("2020-01-05")
            .withSubject("test")
            .withEventOrgUnitOrganizer("Organizer")
            .withEventProjectSponsor("Sponsor")
            .build();

        Item secondEvent = ItemBuilder.createItem(context, collection)
            .withEntityType("Event")
            .withTitle("Second Event")
            .withType("Workshop")
            .withAcronym("SE")
            .withEventPlace("Terni")
            .withEventStartDate("2021-01-01")
            .withEventEndDate("2021-01-05")
            .withEventProjectOrganizer("Organizer")
            .build();

        Item thirdEvent = ItemBuilder.createItem(context, collection)
            .withEntityType("Event")
            .withTitle("Third Event")
            .withType("Unspecified")
            .withAcronym("TE")
            .withEventPlace("Rome")
            .withEventCountry("Italy")
            .withEventStartDate("2020-06-01")
            .withEventEndDate("2020-06-05")
            .withEventProjectSponsor("Sponsor")
            .withEventProjectPartner("Partner")
            .build();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("event-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstEvent, secondEvent, thirdEvent).iterator(), out);

        try (FileInputStream fis = getFileInputStream("events.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void testVirtualFieldDate() throws Exception {

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withIssueDate("2020-02-14")
            .withDateAccepted("2021")
            .withDateAccepted("2022")
            .withDateAccepted("2023")
            .build();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkVirtualFieldDate", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String currentYear = new SimpleDateFormat("yyyy").format(new Date());

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(12));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"only-year\": \"2020\","));
        assertThat(resultLines[2].trim(), is("\"date-without-time\": \"2020-02-14\","));
        assertThat(resultLines[3].trim(), is("\"another-date-without-time\": \"2020\\/02\\/14\","));
        assertThat(resultLines[4].trim(), is("\"date-with-time\": \"14-02-2020 00:00:00\","));
        assertThat(resultLines[5].trim(), is("\"another-date-with-time\": \"20200214 000000\","));
        assertThat(resultLines[6].trim(), is("\"current-timestamp\": \"" + currentDate + "\","));
        assertThat(resultLines[7].trim(), is("\"current-year\": \"" + currentYear + "\","));
        assertThat(resultLines[8].trim(), is("\"repeatable-date\": \"2021\","));
        assertThat(resultLines[9].trim(), is("\"repeatable-date\": \"2022\","));
        assertThat(resultLines[10].trim(), is("\"repeatable-date\": \"2023\""));
        assertThat(resultLines[11].trim(), is("}"));

    }

    @Test
    public void testVirtualFieldVocabulary() throws Exception {

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withType("Resource Type Genres::software::research software")
            .build();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkVirtualFieldVocabulary", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"first-element\": \"Resource Type Genres\","));
        assertThat(resultLines[2].trim(), is("\"second-element\": \"software\","));
        assertThat(resultLines[3].trim(), is("\"last-element\": \"research software\","));
        assertThat(resultLines[4].trim(), is("\"second-last-element\": \"software\","));
        assertThat(resultLines[5].trim(), is("\"deep-element\": \"research software\""));
        assertThat(resultLines[6].trim(), is("}"));

    }

    @Test
    public void placeholderFieldMustBeReplacedWithEmptyStringTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Item patent = ItemBuilder.createItem(context, collection)
                                 .withTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
                                 .withEntityType("Patent").build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("patent-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, patent, out);

        String json = out.toString();
        JSONObject obj = new JSONObject(json);
        assertTrue(obj.has("title"));
        assertTrue(StringUtils.equals(obj.getString("title"), StringUtils.EMPTY));
    }

    @Test
    public void xmlDisseminateMetadataSecurityFirstLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();
        context.setCurrentUser(null);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications2.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void xmlDisseminateMetadataSecuritySecondLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                      .withEmail("test-email@test.com")
                                      .withPassword(password)
                                      .withNameInMetadata("Bob", "Charlton")
                                      .build();

        GroupBuilder.createGroup(context)
                    .withName("Trusted")
                    .addMember(user)
                    .build();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();

        // disseminate with user that belongs to 'Trusted' group
        context.setCurrentUser(user);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications3.xml")) {
            compareEachLine(out.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }

        // disseminate with user that does not belongs to 'Trusted' group
        context.setCurrentUser(eperson);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out2);

        try (FileInputStream fis = getFileInputStream("publications2.xml")) {
            compareEachLine(out2.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }
    }

    @Test
    public void xmlDisseminateMetadataSecurityThirdLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
                                      .withEmail("test-email@test.com")
                                      .withPassword(password)
                                      .withNameInMetadata("Bob", "Charlton")
                                      .build();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withCrisOwner("Owner", owner.getID().toString())
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        CrisLayoutBox box1 = CrisLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        CrisLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();

        // disseminate with admin
        context.setCurrentUser(admin);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications4.xml")) {
            compareEachLine(out.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }

        // disseminate with owner of item
        context.setCurrentUser(owner);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out2);

        try (FileInputStream fis = getFileInputStream("publications4.xml")) {
            compareEachLine(out2.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }
    }

    private void compareEachLine(String result, String expectedResult) {

        String[] resultLines = result.split("\n");
        String[] expectedResultLines = expectedResult.split("\n");

        assertThat(sameLineCountReason(resultLines, expectedResultLines),
            resultLines.length, equalTo(expectedResultLines.length));

        for (int i = 0; i < resultLines.length; i++) {
            assertThat(removeTabs(resultLines[i]), equalTo(removeTabs(expectedResultLines[i])));
        }
    }

    private String sameLineCountReason(String[] resultLines, String[] expectedResultLines) {
        String message = "The result should have the same lines number of the expected result.";
        String result = String.join("\n", resultLines);
        String expectedResult = String.join("\n", expectedResultLines);
        return message + "\nExpected:\n" + expectedResult + "\nActual:\n" + result;
    }

    private String removeTabs(String string) {
        return string != null ? string.replace("\t", "").trim() : null;
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
