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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
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
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link CsvCrosswalkIT}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CsvCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private Community community;

    private Collection collection;

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private CsvCrosswalk csvCrosswalk;

    private DCInputsReader dcInputsReader;

    @Before
    public void setup() throws SQLException, AuthorizeException, DCInputsReaderException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

        dcInputsReader = mock(DCInputsReader.class);

        when(dcInputsReader.hasFormWithName("traditional-oairecerif-identifier-url")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-oairecerif-identifier-url"))
            .thenReturn(Arrays.asList("oairecerif.identifier.url", "crisrp.site.title"));

        when(dcInputsReader.hasFormWithName("traditional-oairecerif-person-affiliation")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-oairecerif-person-affiliation"))
            .thenReturn(Arrays.asList("oairecerif.person.affiliation", "oairecerif.affiliation.startDate",
                "oairecerif.affiliation.endDate", "oairecerif.affiliation.role"));

        when(dcInputsReader.hasFormWithName("traditional-crisrp-education")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-crisrp-education"))
            .thenReturn(Arrays.asList("crisrp.education", "crisrp.education.start",
                "crisrp.education.end", "crisrp.education.role"));

        when(dcInputsReader.hasFormWithName("traditional-crisrp-qualification")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-crisrp-qualification"))
            .thenReturn(Arrays.asList("crisrp.qualification", "crisrp.qualification.start",
                "crisrp.qualification.end"));

        when(dcInputsReader.hasFormWithName("traditional-dc-contributor-author")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-dc-contributor-author"))
            .thenReturn(Arrays.asList("dc.contributor.author", "oairecerif.author.affiliation"));

        when(dcInputsReader.hasFormWithName("traditional-dc-contributor-editor")).thenReturn(true);
        when(dcInputsReader.getAllFieldNamesByFormName("traditional-dc-contributor-editor"))
            .thenReturn(Arrays.asList("dc.contributor.editor", "oairecerif.editor.affiliation"));

    }

    @After
    public void after() throws DCInputsReaderException {
        if (this.csvCrosswalk != null) {
            this.csvCrosswalk.setDCInputsReader(new DCInputsReader());
        }
    }

    @Test
    public void testDisseminateManyPersons() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullPersonItem();

        Item secondItem = createItem(context, collection)
            .withRelationshipType("Person")
            .withTitle("Edward Red")
            .withGivenName("Edward")
            .withFamilyName("Red")
            .withBirthDate("1982-05-21")
            .withGender("M")
            .withPersonAffiliation("OrgUnit")
            .withPersonAffiliationStartDate("2015-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item thirdItem = createItem(context, collection)
            .withTitle("Adam White")
            .withRelationshipType("Person")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University of Rome")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("person-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    @Test
    public void testDisseminateSinglePerson() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withRelationshipType("Person")
            .withTitle("Walter White")
            .withVariantName("Heisenberg")
            .withVariantName("W.W.")
            .withGivenName("Walter")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Professor")
            .withPersonMainAffiliation("High School")
            .withPersonKnowsLanguages("English")
            .withPersonEducation("School")
            .withPersonEducationStartDate("1968-09-01")
            .withPersonEducationEndDate("1973-06-10")
            .withPersonEducationRole("Student")
            .withPersonEducation("University")
            .withPersonEducationStartDate("1980-09-01")
            .withPersonEducationEndDate("1985-06-10")
            .withPersonEducationRole("Student")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonQualification("Qualification")
            .withPersonQualificationStartDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("person-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("person.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }

    }

    @Test
    public void testDisseminatePublications() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullPublicationItem();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withTitle("Second Publication")
            .withDoiIdentifier("doi:222.222/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::learning object")
            .withIssueDate("2019-12-31")
            .withAuthor("Edward Smith")
            .withAuthorAffiliation("Company")
            .withAuthor("Walter White")
            .withVolume("V-02")
            .withCitationStartPage("1")
            .withCitationEndPage("20")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withTitle("Another Publication")
            .withDoiIdentifier("doi:333.333/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::clinical trial")
            .withIssueDate("2010-02-01")
            .withAuthor("Jessie Pinkman")
            .withDescriptionAbstract("Description of publication")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("publication-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    @Test
    public void testDisseminateProjects() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createFullProjectItem();

        Item secondItem = ItemBuilder.createItem(context, collection)
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

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Project")
            .withAcronym("TTP")
            .withTitle("Third Test Project")
            .withOpenaireId("88-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Third Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withSubject("project")
            .withSubject("test")
            .withOAMandate("false")
            .withOAMandateURL("www.oamandate.com")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("project-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("projects.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    @Test
    public void testDisseminateOrgUnits() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("OrgUnit")
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

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("OrgUnit")
            .withAcronym("TTOU")
            .withTitle("Third Test OrgUnit")
            .withType("Private non-profit")
            .withOrgUnitIdentifier("ID-03")
            .withUrlIdentifier("www.orgUnit.test")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("orgUnit-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("orgUnits.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    @Test
    public void testDisseminateEquipments() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Equipment")
            .withAcronym("FT-EQ")
            .withTitle("First Test Equipment")
            .withInternalId("ID-01")
            .withDescription("This is an equipment to test the export functionality")
            .withEquipmentOwnerOrgUnit("Test OrgUnit")
            .withEquipmentOwnerPerson("Walter White")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Equipment")
            .withAcronym("ST-EQ")
            .withTitle("Second Test Equipment")
            .withInternalId("ID-02")
            .withDescription("This is another equipment to test the export functionality")
            .withEquipmentOwnerPerson("John Smith")
            .build();

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Equipment")
            .withAcronym("TT-EQ")
            .withTitle("Third Test Equipment")
            .withInternalId("ID-03")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("equipment-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("equipments.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    @Test
    public void testDisseminateFundings() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
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

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
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

        Item thirdItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Funding")
            .withAcronym("TT-FU")
            .withTitle("Third Test Funding")
            .withType("Grant")
            .withInternalId("ID-03")
            .withFundingIdentifier("0003")
            .withAmount("20.000,00")
            .withAmountCurrency("EUR")
            .withFundingEndDate("2010-01-01")
            .withOAMandate("false")
            .withOAMandateURL("www.mandate.com")
            .build();

        context.restoreAuthSystemState();

        csvCrosswalk = (CsvCrosswalk) crosswalkMapper.getByType("funding-csv");
        assertThat(csvCrosswalk, notNullValue());
        csvCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csvCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("fundings.csv")) {
            String expectedCsv = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedCsv));
        }
    }

    private Item createFullPersonItem() {
        Item item = createItem(context, collection)
            .withTitle("John Smith")
            .withRelationshipType("Person")
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
            .withScopusAuthorIdentifier("111")
            .withResearcherIdentifier("r1")
            .withResearcherIdentifier("r2")
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
        return item;
    }

    private Item createFullPublicationItem() {
        return ItemBuilder.createItem(context, collection)
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
            .withRelationConference("The best Conference")
            .withRelationDataset("DataSet")
            .build();
    }

    private Item createFullProjectItem() {
        return ItemBuilder.createItem(context, collection)
            .withRelationshipType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withProjectStartDate("2020-01-01")
            .withProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit")
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
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
