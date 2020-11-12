/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.util.Arrays.asList;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkimport.utils.WorkbookUtils;
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

        try (FileOutputStream fos = new FileOutputStream("/home/luca/Scrivania/test.xls")) {
            workbook.write(fos);
        }

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
            "dc.identifier.citation", "dc.description", "dc.description.sponsorship" };
        String[] mainSheetFirstRow = { firstItemId, "doi:111.111/publication", "99999999",
            "111-222-333", "", "", "", "2049-3630", "", "", "", "http://localhost:4000/handle/123456789/001",
            "978-3-16-148410-0", "Test Publication", "Alternative publication title", "2020-01-01",
            "Controlled Vocabulary for Resource Type Genres::text::review", "en::2::500", "test||export",
            "Description Abstract", "Published in publication", "ISBN-01", "doi:10.3972/test", "Journal", "", "", "",
            "", "", "", "", "", "", "", "The best Conference", "DataSet", "", "Description", "" };
        String[] mainSheetSecondRow = { secondItemId, "", "SCOPUS-002", "ISI-002", "", "",
            "", "ISSN-002||ISSN-003", "", "", "", "http://localhost:4000/handle/123456789/002", "ISBN-002",
            "Second Publication", "", "2020-01-01", "Controlled Vocabulary for Resource Type Genres::text::review", "",
            "export", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Conference1||Conference2", "DataSet",
            "CIT-01", "Publication Description", "" };

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
        String[] projectSheetFirstRow = { firstItemId, "Test Project::d9471fee-34fa-4a39-9658-443c4bb47b22::600", "" };
        String[] projectSheetSecondRow = { secondItemId, "Test Project", "01" };

        asserThatSheetHas(projectSheet, "dc.relation.project", 3, asList(projectSheetHeader, projectSheetFirstRow,
            projectSheetSecondRow));
    }

    private void asserThatSheetHas(Sheet sheet, String name, int rowsNumber, List<String[]> rows) {
        assertThat(sheet.getSheetName(), equalTo(name));
        assertThat(sheet.getPhysicalNumberOfRows(), equalTo(rowsNumber));
        int rowCount = 0;
        for (String[] row : rows) {
            assertThat(getRowValues(sheet.getRow(rowCount++), row.length), contains(row));
        }

    }

    private List<String> getRowValues(Row row, int size) {
        List<String> values = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            values.add(WorkbookUtils.getCellValue(row, i));
        }
        return values;
    }

}
