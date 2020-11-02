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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.utils.DSpace;
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
            .withRelationshipType("Person")
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
    public void testPersonWithEmptyGroupsXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Person")
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
            .withRelationshipType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-04-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
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
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationDataset("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-xml");
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
            .withRelationshipType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-04-01")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withRelationshipType("OrgUnit")
            .withTitle("Test Funder")
            .withAcronym("TFO")
            .build();

        ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder", orgUnit.getID().toString())
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withTitle("Test Publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book::book part")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationDataset("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-xml");
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
            .withRelationshipType("Publication")
            .withTitle("First Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::learning object")
            .withIssueDate("2019-12-31")
            .withAuthor("Edward Smith")
            .withAuthorAffiliation("Company")
            .withAuthor("Walter White")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withTitle("Test Funding")
            .withType("Contract")
            .withFunder("Test Funder")
            .withAcronym("TF-01")
            .build();

        Item secondPublication = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withTitle("Second Publication")
            .withDoiIdentifier("doi:222.222/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::clinical trial")
            .withIssueDate("2010-02-01")
            .withAuthor("Jessie Pinkman")
            .withRelationFunding("Test Funding", funding.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-xml");
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
            .withRelationshipType("Publication")
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
            .withRelationshipType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Project")
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
            .withRelationshipType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-xml");
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
            .withRelationshipType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Project")
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
            .withRelationshipType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
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
            .withRelationshipType("Project")
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
            .withRelationshipType("Project")
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

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-xml");
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
            .withRelationshipType("Project")
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
            .withRelationshipType("Project")
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

    private void compareEachLine(String result, String expectedResult) {

        String[] resultLines = result.split("\n");
        String[] expectedResultLines = expectedResult.split("\n");

        assertThat("The result should have the same lines number of the expected result",
            resultLines.length, equalTo(expectedResultLines.length));

        for (int i = 0; i < resultLines.length; i++) {
            assertThat(resultLines[i], equalTo(expectedResultLines[i]));
        }
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
