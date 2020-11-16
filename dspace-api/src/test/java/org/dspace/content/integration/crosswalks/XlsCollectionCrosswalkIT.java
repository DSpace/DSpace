/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.util.Arrays.asList;
import static org.dspace.app.bulkimport.utils.WorkbookUtils.getRowValues;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.CrisConstants;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link XlsCollectionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class XlsCollectionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private XlsCollectionCrosswalk xlsCollectionCrosswalk;

    private Community community;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        StreamDisseminationCrosswalkMapper crosswalkMapper = new DSpace()
            .getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        xlsCollectionCrosswalk = (XlsCollectionCrosswalk) crosswalkMapper.getByType("collection-xls");

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withHandle("123456789/001")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withRelationIsbn("ISBN-01")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withRelationProject("Test Project", "d9471fee-34fa-4a39-9658-443c4bb47b22")
            .withRelationGrantno(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withRelationFunding("Test Funding")
            .withRelationConference("The best Conference")
            .withRelationDataset("DataSet")
            .withDescription("Description")
            .withDescriptionAbstract("Description Abstract")
            .withIsPartOf("Journal")
            .build();

        Item secondItem = ItemBuilder.createItem(context, collection)
            .withRelationshipType("Publication")
            .withHandle("123456789/002")
            .withTitle("Second Publication")
            .withIsbnIdentifier("ISBN-002")
            .withIssnIdentifier("ISSN-002")
            .withIssnIdentifier("ISSN-003")
            .withIsiIdentifier("ISI-002")
            .withScopusIdentifier("SCOPUS-002")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("Jesse Pinkman")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project")
            .withRelationGrantno("01")
            .withRelationConference("Conference1")
            .withRelationConference("Conference2")
            .withRelationDataset("DataSet")
            .withDescription("Publication Description")
            .withCitationIdentifier("CIT-01")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(4));

        String firstItemId = firstItem.getID().toString();
        String secondItemId = secondItem.getID().toString();

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi",
            "dc.identifier.adsbibcode", "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn",
            "dc.identifier.other", "dc.identifier.ismn", "dc.identifier.govdoc",
            "dc.identifier.uri", "dc.identifier.isbn", "dc.title", "dc.title.alternative", "dc.date.issued",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.relation.publication",
            "dc.relation.isbn", "dc.relation.doi", "dc.relation.ispartof", "dc.relation.ispartofseries",
            "dc.relation.issn", "dc.coverage.publication", "dc.coverage.isbn", "dc.coverage.doi",
            "dc.description.sponsorship", "dc.description.volume", "dc.description.issue", "dc.description.startpage",
            "dc.description.endpage", "dc.relation.conference", "dc.relation.dataset",
            "dc.identifier.citation", "dc.description" };
        String[] mainSheetFirstRow = { firstItemId, "doi:111.111/publication", "99999999",
            "111-222-333", "", "", "", "2049-3630", "", "", "", "http://localhost:4000/handle/123456789/001",
            "978-3-16-148410-0", "Test Publication", "Alternative publication title", "2020-01-01",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en$$2$$500", "test||export",
            "Description Abstract", "Published in publication", "ISBN-01", "doi:10.3972/test", "Journal", "", "", "",
            "", "", "", "", "", "", "", "The best Conference", "DataSet", "", "Description" };
        String[] mainSheetSecondRow = { secondItemId, "", "SCOPUS-002", "ISI-002", "", "",
            "", "ISSN-002||ISSN-003", "", "", "", "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Second Publication", "", "2020-01-01", "Controlled Vocabulary for Resource Type Genres::text::review", "",
            "export", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Conference1||Conference2", "DataSet",
            "CIT-01", "Publication Description" };

        asserThatSheetHas(mainSheet, "items", 3, Arrays.asList(mainSheetHeader, mainSheetFirstRow, mainSheetSecondRow));

        Sheet authorSheet = workbook.getSheetAt(1);
        String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
        String[] authorSheetFirstRow = { firstItemId, "John Smith", "" };
        String[] authorSheetSecondRow = { firstItemId, "Walter White", "Company" };
        String[] authorSheetThirdRow = { secondItemId, "Jesse Pinkman", "Company" };

        asserThatSheetHas(authorSheet, "dc.contributor.author", 4, asList(authorSheetHeader, authorSheetFirstRow,
            authorSheetSecondRow, authorSheetThirdRow));

        Sheet editorSheet = workbook.getSheetAt(2);
        String[] editorSheetHeader = { "PARENT-ID", "dc.contributor.editor", "oairecerif.editor.affiliation" };
        String[] editorSheetFirstRow = { secondItemId, "Editor", "Editor Affiliation" };

        asserThatSheetHas(editorSheet, "dc.contributor.editor", 2, asList(editorSheetHeader, editorSheetFirstRow));

        Sheet projectSheet = workbook.getSheetAt(3);
        String[] projectSheetHeader = { "PARENT-ID", "dc.relation.project", "dc.relation.grantno" };
        String[] projectSheetFirstRow = { firstItemId, "Test Project$$d9471fee-34fa-4a39-9658-443c4bb47b22$$600", "" };
        String[] projectSheetSecondRow = { secondItemId, "Test Project", "01" };

        asserThatSheetHas(projectSheet, "dc.relation.project", 3, asList(projectSheetHeader, projectSheetFirstRow,
            projectSheetSecondRow));
    }

    @Test
    public void testDisseminateWithEmptyCollection() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection collection = createCollection(context, community)
            .withAdminGroup(eperson)
            .build();
        context.restoreAuthSystemState();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xlsCollectionCrosswalk.disseminate(context, collection, baos);

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));
        assertThat(workbook.getNumberOfSheets(), equalTo(1));

        Sheet mainSheet = workbook.getSheetAt(0);
        String[] mainSheetHeader = { "ID", "dc.contributor.author", "dc.title", "dc.title.alternative",
            "dc.date.issued", "dc.publisher", "dc.identifier.citation", "dc.relation.ispartofseries",
            "dc.identifier.doi", "dc.identifier.scopus", "dc.identifier.isi", "dc.identifier.adsbibcode",
            "dc.identifier.pmid", "dc.identifier.arxiv", "dc.identifier.issn", "dc.identifier.other",
            "dc.identifier.ismn", "dc.identifier.govdoc", "dc.identifier.uri", "dc.identifier.isbn",
            "dc.type", "dc.language.iso", "dc.subject", "dc.description.abstract", "dc.description.sponsorship",
            "dc.description" };
        assertThat(mainSheet.getPhysicalNumberOfRows(), equalTo(1));
        assertThat(getRowValues(mainSheet.getRow(0), mainSheetHeader.length), contains(mainSheetHeader));
    }

    @Test
    public void testDisseminateWithMockSubmissionFormConfiguration() throws Exception {

        try {
            DCInputsReader reader = mock(DCInputsReader.class);

            context.turnOffAuthorisationSystem();

            Collection collection = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            List<String> publicationMetadataFields = asList("dc.title", "dc.date.issued", "dc.subject");
            List<String> publicationMetadataFieldGroups = asList("dc.contributor.author");
            List<String> authorGroup = asList("dc.contributor.author", "oairecerif.author.affiliation");

            when(reader.getLanguagesForMetadata(collection, "dc.title")).thenReturn(Arrays.asList("en", "it"));
            when(reader.getSubmissionFormMetadata(collection)).thenReturn(publicationMetadataFields);
            when(reader.getSubmissionFormMetadataGroups(collection)).thenReturn(publicationMetadataFieldGroups);
            when(reader.getAllNestedMetadataByGroupName(collection, "dc.contributor.author")).thenReturn(authorGroup);

            xlsCollectionCrosswalk.setReader(reader);

            Item firstPublication = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Publication")
                .withTitle("First publication")
                .withTitleForLanguage("Prima pubblicazione", "it")
                .withTitleForLanguage("Primera publicacion", "es")
                .withIssueDate("2020-01-01")
                .withAuthor("Walter White", "0ecd5452-aae2-4c18-9aec-0471bdcbadbc")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Jesse Pinkman")
                .withAuthorAffiliation("Company")
                .build();

            Item secondPublication = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Publication")
                .withTitleForLanguage("Second publication", "en")
                .withIssueDate("2019-01-01")
                .build();

            Item thirdPublication = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Publication")
                .withTitle("Third publication")
                .withTitleForLanguage("Terza pubblicazione", "it")
                .withIssueDate("2018-01-01")
                .withAuthor("Carl Johnson")
                .build();

            Item fourthPublication = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Publication")
                .withTitle("Fourth publication")
                .withIssueDate("2017-01-01")
                .withAuthor("Carl Johnson")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withAuthor("Red White")
                .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
                .withSubject("test")
                .withSubject("export")
                .build();

            context.restoreAuthSystemState();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            xlsCollectionCrosswalk.disseminate(context, collection, baos);

            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(baos.toByteArray()));

            assertThat(workbook.getNumberOfSheets(), equalTo(2));

            String firstId = firstPublication.getID().toString();
            String secondId = secondPublication.getID().toString();
            String thirdId = thirdPublication.getID().toString();
            String fourthId = fourthPublication.getID().toString();

            Sheet mainSheet = workbook.getSheetAt(0);
            String[] header = { "ID", "dc.title", "dc.date.issued", "dc.subject", "dc.title[it]", "dc.title[en]" };
            String[] firstRow = { firstId, "First publication", "2020-01-01", "", "Prima pubblicazione", "" };
            String[] secondRow = { secondId, "", "2019-01-01", "", "", "Second publication" };
            String[] thirdRow = { thirdId, "Third publication", "2018-01-01", "", "Terza pubblicazione", "" };
            String[] fourthRow = { fourthId, "Fourth publication", "2017-01-01", "test||export", "", "" };

            asserThatSheetHas(mainSheet, "items", 5, asList(header, firstRow, secondRow, thirdRow, fourthRow));

            Sheet authorSheet = workbook.getSheetAt(1);
            String[] authorSheetHeader = { "PARENT-ID", "dc.contributor.author", "oairecerif.author.affiliation" };
            String[] authorSheetFirstRow = { firstId, "Walter White$$0ecd5452-aae2-4c18-9aec-0471bdcbadbc$$600", "" };
            String[] authorSheetSecondRow = { firstId, "Jesse Pinkman", "Company" };
            String[] authorSheetThirdRow = { thirdId, "Carl Johnson", "" };
            String[] authorSheetFourthRow = { fourthId, "Carl Johnson", "" };
            String[] authorSheetFifthRow = { fourthId, "Red White", "" };

            asserThatSheetHas(authorSheet, "dc.contributor.author", 6, asList(authorSheetHeader, authorSheetFirstRow,
                authorSheetSecondRow, authorSheetThirdRow, authorSheetFourthRow, authorSheetFifthRow));

        } finally {
            this.xlsCollectionCrosswalk.setReader(new DCInputsReader());
        }

    }

    private void asserThatSheetHas(Sheet sheet, String name, int rowsNumber, List<String[]> rows) {
        assertThat(sheet.getSheetName(), equalTo(name));
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(rowsNumber));
        int rowCount = 0;
        for (String[] row : rows) {
            assertThat(getRowValues(sheet.getRow(rowCount++), row.length), contains(row));
        }
    }

}
