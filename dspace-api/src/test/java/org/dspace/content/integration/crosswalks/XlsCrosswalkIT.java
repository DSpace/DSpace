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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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
 * Integration tests for {@link XlsCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private Community community;

    private Collection collection;

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private XlsCrosswalk xlsCrosswalk;

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
        if (this.xlsCrosswalk != null) {
            this.xlsCrosswalk.setDCInputsReader(new DCInputsReader());
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
            .withRelationshipType("Person")
            .withTitle("Adam White")
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

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("person-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)), contains("Preferred name", "Full name", "Vernacular name", "Variants",
            "Given name", "Family name", "Birth-date", "Gender", "Job title", "Main affiliation", "Working groups",
            "Personal sites", "Email", "Interests", "ORCID", "Scopus author ids", "Researcher ids", "Affiliations",
            "Biography", "Educations", "Country", "Qualifications", "Knows languages"));

        assertThat(getRowValues(sheet.getRow(1)), contains("John Smith", "John Smith", "JOHN SMITH", "J.S.||Smith John",
            "John", "Smith", "1992-06-26", "M", "Researcher", "University", "First work group||Second work group",
            "www.test.com/Test||www.john-smith.com||www.site.com/Site", "test@test.com", "Science",
            "0000-0002-9079-5932", "111", "r1||r2", "Company/2018-01-01//Developer",
            "Biography: \n\t\"This is my biography\"", "School/2000-01-01/2005-01-01/Student", "England",
            "First Qualification/2015-01-01/2016-01-01||Second Qualification/2016-01-02", "English||Italian"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Edward Red", "", "", "",
            "Edward", "Red", "1982-05-21", "M", "", "", "", "", "", "", "", "", "", "OrgUnit/2015-01-01//Developer", "",
            "", "", "", ""));

        assertThat(getRowValues(sheet.getRow(3)), contains("Adam White", "", "", "", "Adam", "White", "1962-03-23", "M",
            "Researcher", "University of Rome", "", "", "", "", "", "", "", "", "",
            "School/2000-01-01/2005-01-01/Student", "", "", "English||Italian"));

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

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("person-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, item, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));

        assertThat(getRowValues(sheet.getRow(0)), contains("Preferred name", "Full name", "Vernacular name", "Variants",
            "Given name", "Family name", "Birth-date", "Gender", "Job title", "Main affiliation", "Working groups",
            "Personal sites", "Email", "Interests", "ORCID", "Scopus author ids", "Researcher ids", "Affiliations",
            "Biography", "Educations", "Country", "Qualifications", "Knows languages"));

        assertThat(getRowValues(sheet.getRow(1)), contains("Walter White", "", "", "Heisenberg||W.W.",
            "Walter", "White", "1962-03-23", "M", "Professor", "High School", "", "", "", "", "0000-0002-9079-5932",
            "", "", "", "", "School/1968-09-01/1973-06-10/Student||University/1980-09-01/1985-06-10/Student", "",
            "Qualification", "English"));

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

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("publication-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)), contains("Title", "Subtitle", "Type", "Language", "Publication date",
            "Part of", "Journal or Serie", "ISBN (of the container)", "ISSN (of the container)",
            "DOI (of the container)", "Publisher", "DOI", "ISBN", "ISSN", "ISI-Number", "SCP-Number", "Volume", "Issue",
            "Start page", "End page", "Authors", "Editors", "Abstract", "Event", "Product"));

        assertThat(getRowValues(sheet.getRow(1)), contains("Test Publication", "Alternative publication title",
            "http://purl.org/coar/resource_type/c_efa0", "en", "2020-01-01", "Published in publication", "", "", "",
            "doi:10.3972/test", "Publication publisher", "doi:111.111/publication", "978-3-16-148410-0", "2049-3630",
            "111-222-333", "99999999", "V.01", "Issue", "", "", "John Smith||Walter White/Company",
            "Editor/Editor Affiliation", "", "The best Conference", "DataSet"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Second Publication", "",
            "http://purl.org/coar/resource_type/c_e059", "", "2019-12-31", "", "", "", "", "", "",
            "doi:222.222/publication", "", "", "", "", "V-02", "", "1", "20", "Edward Smith/Company||Walter White", "",
            "", "", ""));

        assertThat(getRowValues(sheet.getRow(3)), contains("Another Publication", "",
            "http://purl.org/coar/resource_type/c_cb28", "", "2010-02-01", "", "", "", "", "", "",
            "doi:333.333/publication", "", "", "", "", "", "", "", "", "Jessie Pinkman", "",
            "Description of publication", "", ""));

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

        xlsCrosswalk = (XlsCrosswalk) crosswalkMapper.getByType("project-xls");
        assertThat(xlsCrosswalk, notNullValue());
        xlsCrosswalk.setDCInputsReader(dcInputsReader);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem, thirdItem).iterator(), baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(4));

        assertThat(getRowValues(sheet.getRow(0)), contains("Title", "Acronym", "OpenAIRE id(s)", "URL(s)", "Start date",
            "End date", "Status", "Coordinator(s)", "Partner Organization(s)", "Participant Organization(s)",
            "Project Coordinator", "Co-Investigator(s)", "Uses equipment(s)", "Keyword(s)", "Description", "OA Mandate",
            "OA Policy URL"));

        assertThat(getRowValues(sheet.getRow(1)), contains("Test Project", "TP", "11-22-33", "www.project.test",
            "2020-01-01", "2020-12-31", "OPEN", "Coordinator OrgUnit", "Partner OrgUnit||Another Partner OrgUnit",
            "First Member OrgUnit||Second Member OrgUnit||Third Member OrgUnit", "Investigator",
            "First coinvestigator||Second coinvestigator", "Test equipment", "project||test",
            "This is a project to test the export", "true", "oamandate-url"));

        assertThat(getRowValues(sheet.getRow(2)), contains("Second Test Project", "STP", "55-66-77||11-33-22",
            "www.project.test", "2010-01-01", "2012-12-31", "Status", "Second Coordinator OrgUnit", "", "",
            "Second investigator", "Coinvestigator", "Another test equipment", "", "", "", "oamandate"));

        assertThat(getRowValues(sheet.getRow(3)), contains("Third Test Project", "TTP", "88-22-33", "www.project.test",
            "2020-01-01", "2020-12-31", "OPEN", "Third Coordinator OrgUnit", "Partner OrgUnit", "Member OrgUnit",
            "Investigator", "First coinvestigator||Second coinvestigator", "", "project||test", "", "false",
            "www.oamandate.com"));
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

    private List<String> getRowValues(Row row) {
        return StreamSupport.stream(row.spliterator(), false)
            .map(cell -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue())
            .collect(Collectors.toList());
    }
}
