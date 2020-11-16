/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static java.lang.String.join;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authority.CrisConsumer;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.CrisConstants;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration tests for {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Ignore
public class BulkImportIT extends AbstractIntegrationTestWithDatabase {

    private static final String CRIS_CONSUMER = CrisConsumer.CONSUMER_NAME;

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

        String fileLocation = getXlsFilePath("empty.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testEmptyHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The header of sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testOneHeaderEmptyImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("one-header-empty.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[Empty metadata]"));
    }

    @Test
    public void testWithoutHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("without-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Wrong ID header on sheet Main Entity: RID::123456789"));
    }

    @Test
    public void testInvalidHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[unknown is not valid for the given collection, "
            + "person.identifier is not valid for the given collection]"));
    }

    @Test
    public void testInvalidSheetNameImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-sheet-name.xlsx");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet name wrongdc.contributor.author "
            + "is not a valid metadata group"));
    }

    @Test
    public void testMetadataGroupRowWithManyValuesImport() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("metadata-group-row-with-many-values.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Multiple metadata value on the same cell not allowed in the "
            + "metadata group sheets: Author1 || Author2"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info message", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 2 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));
        assertThat(infoMessages.get(4), containsString("Row 3 - Item created successfully"));
    }

    @Test
    public void testHeadersDuplicatedImport() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("headers-duplicated.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet Main Entity - Duplicated headers found "
            + "on cells 3 and 4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

        String id = getIdFromCreatedMessage(infoMessages.get(3), 2);
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Luca Stone", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", null, null, 2, -1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher", null, null, 0, -1)));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher", null, null, 0, -1)));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333", null, null, 0, -1))));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithAuthority() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 2 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

        String id = getIdFromCreatedMessage(infoMessages.get(3), 2);
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author1", null, "authority1", 0, 600)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit1", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit2", null, "authority2", 1, 400)));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImport() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToUpdateByRid = createItem(context, publications)
            .withTitle("My Publication")
            .withTitleForLanguage("My Publication English", "en")
            .withResearcherIdentifier("123456789")
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 6 info messages", infoMessages, hasSize(6));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));
        assertThat(infoMessages.get(4), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(5), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183", null, null, 0, -1)));

        // verify updated item (ROW 3)
        Item itemUpdated = itemService.find(context, itemToUpdateByRid.getID());
        metadata = itemUpdated.getMetadata();

        assertThat(metadata, hasItems(with("dc.title", "Publication", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "Publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.title", "English Publication", "en", null, 2, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "01/07/95", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "John Smith", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/184", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.relation.project", "Test Project", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.relation.grantno", "1", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.relation.project", "Another Test Project", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.relation.grantno", PLACEHOLDER, null, null, 1, -1)));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImportWithErrorAndNotAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));
        assertThat(infoMessages.get(4), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183", null, null, 0, -1)));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManyPublicationImportWithErrorAndAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item itemToDelete = createItem(context, publications).withDoiIdentifier("10.1000/182").build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("many-publications.xls");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

        // verify created item (ROW 2)
        String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);
        Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", null, null, 1, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, null, null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183", null, null, 0, -1)));

        // verify deleted item (ROW 4)
        assertThat("Item expected not to be deleted", itemService.find(context, itemToDelete.getID()), notNullValue());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithOneInvalidAuthorityAndNoAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-one-invalid-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid confidence value xxx"));

        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 6 info messages", infoMessages, hasSize(6));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));
        assertThat(infoMessages.get(4), containsString("Row 3 - Item created successfully"));
        assertThat(infoMessages.get(5), containsString("Row 4 - Item created successfully"));

        String id = getIdFromCreatedMessage(infoMessages.get(4), 3);
        Item createdItem = itemService.findByIdOrLegacyId(context, id);
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2", null, null, 0, -1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit2", null, "authority2", 0, 400)));

    }

    @Test
    public void testCreatePublicationWithOneInvalidAuthorityAndAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-one-invalid-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation, "-e" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid confidence value xxx"));

        assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 1 info messages", infoMessages, hasSize(1));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeGeneratedAuthority() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            Item person = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Person")
                .withTitle("Walter White")
                .withOrcidIdentifier("0000-0002-9079-593X")
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-generated-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
            assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
            assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

            // verify created item (ROW 2)
            String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = person.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication", null, null, 0, -1)));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeGeneratedAuthorityAndNoRelatedItemFound() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            createCollection(context, community)
                .withRelationshipType("Person")
                .withAdminGroup(eperson)
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-generated-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
            assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
            assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

            // verify created item (ROW 2)
            String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);

            Item relatedPersonItem = findItemByMetadata("dc", "title", null, "Walter White");
            assertThat("Related Person item expected to be created", relatedPersonItem, notNullValue());

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = relatedPersonItem.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication", null, null, 0, -1)));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeReferencedAuthority() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            Item person = ItemBuilder.createItem(context, collection)
                .withRelationshipType("Person")
                .withTitle("Walter White")
                .withOrcidIdentifier("0000-0002-9079-593X")
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
            assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
            assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

            // verify created item (ROW 2)
            String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            String personId = person.getID().toString();

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication", null, null, 0, -1)));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreatePublicationWithWillBeReferencedAuthorityAndNoRelatedItemFound() throws Exception {
        String[] defaultConsumers = activateCrisConsumer();
        try {

            context.turnOffAuthorisationSystem();

            createCollection(context, community)
                .withRelationshipType("Person")
                .withAdminGroup(eperson)
                .build();

            Collection publications = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            context.commit();
            context.restoreAuthSystemState();

            String publicationCollectionId = publications.getID().toString();
            String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
            String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation, "-e" };
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
            assertThat("Expected no errors", handler.getErrorMessages(), emptyCollectionOf(String.class));
            assertThat("Expected no warnings", handler.getWarningMessages(), emptyCollectionOf(String.class));

            List<String> infoMessages = handler.getInfoMessages();
            assertThat("Expected 4 info messages", infoMessages, hasSize(4));

            assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
            assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
            assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
            assertThat(infoMessages.get(3), containsString("Row 2 - Item created successfully"));

            // verify created item (ROW 2)
            String createdItemId = getIdFromCreatedMessage(infoMessages.get(3), 2);

            Item createdItem = itemService.findByIdOrLegacyId(context, createdItemId);
            assertThat("Item expected to be created", createdItem, notNullValue());

            List<MetadataValue> metadata = createdItem.getMetadata();
            assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null,
                "will be referenced::ORCID::0000-0002-9079-593X", 0, -1)));
            assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication", null, null, 0, -1)));

        } finally {
            resetConsumers(defaultConsumers);
        }
    }

    private String[] activateCrisConsumer() {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String[] consumers = configService.getArrayProperty("event.dispatcher.default.consumers");
        if (!ArrayUtils.contains(consumers, CRIS_CONSUMER)) {
            String newConsumers = consumers.length > 0 ? join(",", consumers) + "," + CRIS_CONSUMER : CRIS_CONSUMER;
            configService.setProperty("event.dispatcher.default.consumers", newConsumers);
            EventService eventService = EventServiceFactory.getInstance().getEventService();
            eventService.reloadConfiguration();
        }

        return consumers;
    }

    private void resetConsumers(String[] consumers) {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configService.setProperty("event.dispatcher.default.consumers", consumers);
    }

    private Item findItemByMetadata(String schema, String element, String qualifier, String value) throws Exception {
        Iterator<Item> iterator = itemService.findArchivedByMetadataField(context, schema, element, qualifier, value);
        return iterator.hasNext() ? iterator.next() : null;
    }


    private String getIdFromCreatedMessage(String message, int row) {
        return message.substring(("Row " + row + " - Item created successfully - ID: ").length());
    }

    private String getXlsFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }
}
