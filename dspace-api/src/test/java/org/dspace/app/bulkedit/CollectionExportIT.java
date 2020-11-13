/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.dspace.app.bulkimport.utils.WorkbookUtils.getRowValues;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CollectionExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CollectionExportIT extends AbstractIntegrationTestWithDatabase {

    private Community community;

    @Before
    public void beforeTests() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testWithUnknownCollection() throws InstantiationException, IllegalAccessException {

        String[] args = new String[] { "collection-export", "-c", "75900f30-8f92-465f-9353-e72280ec8a30" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errors = handler.getErrorMessages();
        assertThat("Expected 1 error message", errors, hasSize(1));
        assertThat(errors.get(0), containsString("No collection found with id 75900f30-8f92-465f-9353-e72280ec8a30"));

    }

    @Test
    public void testWithNotCollectionAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection collection = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test patent")
            .withAuthor("White, Walter")
            .withIssueDate("2020-01-01")
            .withLanguage("it")
            .withSubject("test")
            .withSubject("export")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String[] args = new String[] { "collection-export", "-c", collection.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infos = handler.getInfoMessages();
        assertThat("Expected 1 info message", infos, hasSize(1));
        assertThat(infos.get(0), containsString("Items exported successfully into file named items.xls"));

        File file = new File("items.xls");
        file.deleteOnExit();

        try (FileInputStream fis = new FileInputStream(file)) {

            Workbook workbook = WorkbookFactory.create(fis);
            assertThat(workbook.getNumberOfSheets(), equalTo(1));

            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getPhysicalNumberOfRows(), equalTo(2));
            assertThat(getRowValues(sheet.getRow(0), 18), contains("ID", "dc.title",
                "dcterms.dateAccepted", "dc.date.issued", "dc.contributor.author", "dcterms.rightsHolder",
                "dc.publisher", "dc.identifier.patentno", "dc.identifier.patentnumber", "dc.type",
                "dc.identifier.applicationnumber", "dc.date.filled", "dc.language.iso",
                "dc.subject", "dc.description.abstract", "dc.relation", "dc.relation.patent",
                "dc.relation.references"));
            assertThat(getRowValues(sheet.getRow(1), 18), contains(item.getID().toString(), "Test patent", "",
                "2020-01-01", "White, Walter", "", "", "", "", "", "", "", "it$$6$$500", "test||export", "", "", "",
                ""));

        }


    }
}
