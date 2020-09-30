/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.ListDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.CrisConstants;
import org.junit.Before;
import org.junit.Test;

public class BulkImportIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    private static final String PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private Community community;

    private Collection collection;

    @Before
    public void beforeTests() throws SQLException, AuthorizeException {

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testEmptyImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFile("empty.xls").getAbsolutePath();
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        ListDSpaceRunnableHandler handler = new ListDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", handler.getErrorMessages(), hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testEmptyHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFile("empty-headers.xls").getAbsolutePath();
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        ListDSpaceRunnableHandler handler = new ListDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", handler.getErrorMessages(), hasSize(1));
        assertThat(errorMessages.get(0), containsString("The header of sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testWithoutHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFile("without-headers.xls").getAbsolutePath();
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        ListDSpaceRunnableHandler handler = new ListDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", handler.getErrorMessages(), hasSize(1));
        assertThat(errorMessages.get(0),
            containsString("The following metadata fields of the sheet named Main Entity are invalid"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPersonsImport() throws Exception {

        context.turnOffAuthorisationSystem();

        Item itemToUpdateByRid = createItem(context, collection)
            .withTitle("Tom Hawks")
            .withTitleForLanguage("Tom Hawks English", "en")
            .withRidIdentifier("123456789")
            .build();

        Item itemToDelete = createItem(context, collection).withDoiIdentifier("10.1000/182").build();

        context.restoreAuthSystemState();

        String fileLocation = getXlsFile("many-persons.xls").getAbsolutePath();
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        ListDSpaceRunnableHandler handler = new ListDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));

        assertThat(infoMessages.get(0), containsString("Row 2 - Item created successfully"));
        assertThat(infoMessages.get(1), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(2), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        String createdItemId = infoMessages.get(0).substring("Row 2 - Item created successfully - ID: ".length());
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "John Smith", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "John Smith English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("person.birthDate", "12/12/65", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.person.affiliation", "University", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.person.affiliation", "Another University", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.startDate", "01/01/98", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.startDate", "01/01/01", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.endDate", PLACEHOLDER, null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.endDate", "12/12/05", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.role", "Researcher", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.role", "Researcher", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.identifier.url", "www.test.com", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("crisrp.site.title", "test.com", null, null, 0, -1)));

        // verify updated item (ROW 3)
        Item itemUpdated = itemService.find(context, itemToUpdateByRid.getID());
        metadata = itemUpdated.getMetadata();

        assertThat(metadata, hasItems(with("dc.title", "Carl Johnson", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "Carl Johnson English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.title", "Johnson Carl English", "en", null, 2, -1)));
        assertThat(metadata, hasItems(with("person.birthDate", "01/07/95", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.person.affiliation", "4Science", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.startDate", "01/01/18", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.endDate", PLACEHOLDER, null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.affiliation.role", PLACEHOLDER, null, null, 0, -1)));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    private File getXlsFile(String name) {
        return new File(BASE_XLS_DIR_PATH, name);
    }
}
