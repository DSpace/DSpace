/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.matcher.MetadataValueMatcher.withSecurity;
import static org.dspace.app.matcher.ResourcePolicyMatcher.matches;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.builder.BitstreamBuilder.createBitstream;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.builder.WorkspaceItemBuilder.createWorkspaceItem;
import static org.dspace.core.Constants.ADD;
import static org.dspace.core.Constants.DELETE;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.REMOVE;
import static org.dspace.core.Constants.WRITE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkimport.util.ImportFileUtil;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.matcher.DSpaceObjectMatcher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.CrisConstants;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link BulkImport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings("unchecked")
public class BulkImportIT extends AbstractIntegrationTestWithDatabase {

    protected static final class ImportFileUtilMockClass extends ImportFileUtil {

        public ImportFileUtilMockClass(DSpaceRunnableHandler handler) {
            super(handler);
        }

        @Override
        public InputStream openStream(URL url) {
            return super.openStream(url);
        }
    }

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    private static final String PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private static final Pattern UUID_PATTERN = compile(
        "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    private BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    private SearchService searchService = SearchUtils.getSearchService();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();

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
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testEmptyHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The header of sheet Main Entity of the Workbook is empty"));
    }

    @Test
    public void testOneHeaderEmptyImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("one-header-empty.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[Empty metadata]"));
    }

    @Test
    public void testWithoutHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("without-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Wrong ID header on sheet Main Entity: RID::123456789"));
    }

    @Test
    public void testInvalidHeadersImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-headers.xls");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The following metadata fields of the sheet named "
            + "'Main Entity' are invalid:[unknown is not valid for the given collection, "
            + "person.identifier is not valid for the given collection]"));
    }

    @Test
    public void testInvalidSheetNameImport() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("invalid-sheet-name.xlsx");
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

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
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 2 warning messages", warningMessages, hasSize(2));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));
        assertThat(warningMessages.get(1), containsString("Row 3 - Invalid item left in workspace"));

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Multiple metadata value on the same cell not allowed in the "
            + "metadata group sheets: Author1 || Author2"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info message", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 2 items to process"));
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
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no infos", handler.getInfoMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet Main Entity - Duplicated headers found "
            + "on cells 3 and 4"));
    }

    @Test
    public void testCreatePatent() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));

        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Luca Stone", 1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 2)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, hasItems(with("dc.type", "Patent")));

    }

    @Test
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
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    public void testCreatePublicationWithAuthority() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 2 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));

        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author1", null, "authority1", 0, 600)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit1")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "OrgUnit2", null, "authority2", 1, 400)));

    }

    @Test
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
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

        // verify updated item (ROW 3)
        Item itemUpdated = itemService.find(context, itemToUpdateByRid.getID());
        metadata = itemUpdated.getMetadata();

        assertThat(metadata, hasItems(with("dc.title", "Publication")));
        assertThat(metadata, hasItems(with("dc.title", "Publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.title", "English Publication", "en", null, 2, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "01/07/95")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "John Smith")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER)));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/184")));
        assertThat(metadata, hasItems(with("dc.relation.project", "Test Project")));
        assertThat(metadata, hasItems(with("dc.relation.grantno", "1")));
        assertThat(metadata, hasItems(with("dc.relation.project", "Another Test Project", 1)));
        assertThat(metadata, hasItems(with("dc.relation.grantno", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.type", "Book")));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
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
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 4 - Item deleted successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));

        // verify deleted item (ROW 4)
        assertThat("Item expected to be deleted", itemService.find(context, itemToDelete.getID()), nullValue());

    }

    @Test
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
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item to update found for entity with id RID::123456789"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 6 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "First publication")));
        assertThat(metadata, hasItems(with("dc.title", "First publication English", "en", null, 1, -1)));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Thomas Edison")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Morgan Pitt", 1)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "Company")));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", PLACEHOLDER, 1)));
        assertThat(metadata, hasItems(with("dc.contributor.editor", "Editor")));
        assertThat(metadata, hasItems(with("oairecerif.editor.affiliation", "EditorAffiliation")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));

        // verify deleted item (ROW 4)
        assertThat("Item expected not to be deleted", itemService.find(context, itemToDelete.getID()), notNullValue());

    }

    @Test
    public void testCreatePublicationWithOneInvalidAuthorityAndNoAbortOnError() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-one-invalid-authority.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid security level or confidence value xxx"));

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 3 warning messages", warningMessages, hasSize(3));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));
        assertThat(warningMessages.get(1), containsString("Row 3 - Invalid item left in workspace"));
        assertThat(warningMessages.get(2), containsString("Row 4 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));

        Item createdItem = getItemFromMessage(warningMessages.get(1));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Author2")));
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
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Sheet dc.contributor.author - Row 2 - Invalid metadata "
            + "value Author1$$authority1$$xxx: invalid security level or confidence value xxx"));

        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 1 info messages", infoMessages, hasSize(1));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));

    }

    @Test
    public void testCreatePublicationWithWillBeGeneratedAuthority() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
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
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());

        String personId = person.getID().toString();

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
        assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));
    }

    @Test
    public void testCreatePublicationWithWillBeGeneratedAuthorityAndNoRelatedItemFound() throws Exception {

        context.turnOffAuthorisationSystem();

        createCollection(context, community)
            .withEntityType("Person")
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
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());

        Item relatedPersonItem = findItemByMetadata("dc", "title", null, "Walter White");
        assertThat("Related Person item expected to be created", relatedPersonItem, notNullValue());

        String personId = relatedPersonItem.getID().toString();

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
        assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));
    }

    @Test
    public void testCreatePublicationWithWillBeReferencedAuthority() throws Exception {

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
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
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());

        String personId = person.getID().toString();

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null, personId, 0, 600)));
        assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));
    }

    @Test
    public void testCreatePublicationWithWillBeReferencedAuthorityAndNoRelatedItemFound() throws Exception {

        context.turnOffAuthorisationSystem();

        createCollection(context, community)
            .withEntityType("Person")
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
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 1 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.contributor.author", "Walter White", null,
            "will be referenced::ORCID::0000-0002-9079-593X", 0, -1)));
        assertThat(metadata, hasItems(with("dc.title", "Wonderful Publication")));
    }

    @Test
    @SuppressWarnings({ "deprecation" })
    public void testCreatePublicationInWorkspace() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-workspace-publication.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - WorkspaceItem created successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test publication")));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

    }

    @Test
    public void testCreateArchivedPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-archived-publication.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e", eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));

        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));
        assertThat(createdItem.isDiscoverable(), is(true));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test publication")));
        assertThat(metadata, hasItems(with("dc.date.issued", "12/12/65")));
        assertThat(metadata, hasItems(with("dc.identifier.doi", "10.1000/183")));
        assertThat(metadata, hasItems(with("dc.type", "Article")));

    }

    @Test
    @SuppressWarnings({ "deprecation" })
    public void testUpdateWorkflowPatentWithValidWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .withFulltext("test.txt", null, "test.txt".getBytes())
            .grantLicense()
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - WorkflowItem created successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(updatedItem), nullValue());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateWorkflowPatentWithInvalidWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 4 info messages", infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(updatedItem), notNullValue());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateWorkflowPatentWithoutWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-workflow-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - No workspace item to start found"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateArchivePatentWithWorkspaceItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkspaceItem patentToUpdate = createWorkspaceItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .withFulltext("test.txt", null, "test.txt".getBytes())
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - Item archived successfully "));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(true));

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateArchivePatentWithWorkflowItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        WorkflowItem patentToUpdate = WorkflowItemBuilder.createWorkflowItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - Item archived successfully "));

        Item updatedItem = itemService.find(context, patentToUpdate.getItem().getID());
        assertThat(updatedItem.isArchived(), is(true));

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    @SuppressWarnings("deprecation")
    public void testUpdateArchivePatentWithAlreadyArchivedItem() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .withWorkflowGroup(1, eperson)
            .build();

        Item patentToUpdate = createItem(context, patents)
            .withTitle("Patent to update")
            .withAuthor("Luca G.")
            .withIsniIdentifier("54321")
            .withPatentNo("888-444-333")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-archive-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 5 info messages", infoMessages, hasSize(5));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 2 - No workspace/workflow item to archive found"));

        Item updatedItem = itemService.find(context, patentToUpdate.getID());

        List<MetadataValue> metadata = updatedItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 1)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));
        assertThat(metadata, not(hasItems(with("dc.identifier.patentno", "888-444-333"))));

    }

    @Test
    public void testAutomaticReferenceResolution() throws Exception {

        context.turnOffAuthorisationSystem();

        createCollection(context, community)
            .withEntityType("Person")
            .withAdminGroup(eperson)
            .build();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Collection persons = createCollection(context, community)
            .withSubmissionDefinition("person")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String publicationCollectionId = publications.getID().toString();
        String fileLocation = getXlsFilePath("create-publication-with-will-be-referenced-authority.xls");
        String[] args = new String[] { "bulk-import", "-c", publicationCollectionId, "-f", fileLocation,
            "-e" , eperson.getEmail(), "-er"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());
        assertThat("Expected 4 info messages", handler.getInfoMessages(), hasSize(4));

        assertThat(handler.getInfoMessages().get(0), containsString("Start reading all the metadata group rows"));
        assertThat(handler.getInfoMessages().get(1), containsString("Found 1 metadata groups to process"));
        assertThat(handler.getInfoMessages().get(2), containsString("Found 1 items to process"));
        assertThat(handler.getInfoMessages().get(3), containsString("Row 2 - Item archived successfully"));

        Item publication = getItemFromMessage(handler.getInfoMessages().get(3));
        assertThat("Item expected to be created", publication, notNullValue());

        assertThat(publication.getMetadata(), hasItems(with("dc.contributor.author", "Walter White", null,
            "will be referenced::ORCID::0000-0002-9079-593X", 0, -1)));

        String personsCollectionId = persons.getID().toString();
        fileLocation = getXlsFilePath("create-person.xls");
        args = new String[] { "bulk-import", "-c", personsCollectionId, "-f", fileLocation,
            "-e" , eperson.getEmail(), "-er"};
        handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, admin);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());
        assertThat("Expected 4 info messages", handler.getInfoMessages(), hasSize(4));

        assertThat(handler.getInfoMessages().get(0), containsString("Start reading all the metadata group rows"));
        assertThat(handler.getInfoMessages().get(1), containsString("Found 0 metadata groups to process"));
        assertThat(handler.getInfoMessages().get(2), containsString("Found 1 items to process"));
        assertThat(handler.getInfoMessages().get(3), containsString("Row 2 - Item archived successfully"));

        Item createdPerson = getItemFromMessage(handler.getInfoMessages().get(3));
        publication = context.reloadEntity(publication);

        assertThat(publication.getMetadata(), hasItems(with("dc.contributor.author", "Walter White", null,
            createdPerson.getID().toString(), 0, 600)));

    }

    @Test
    public void testUploadSingleBitstream() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-bitstream-to-item.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), contains(
            containsString("Row 2 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 2 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));

        List<Bitstream> bitstreams = getItemBitstreamsByBundle(item, "TEST-BUNDLE");
        assertThat(bitstreams, hasSize(1));

        Bitstream bitstream = bitstreams.get(0);

        assertThat(bitstream, bitstreamWith("Test title", "test file descr",
            "this is a test file for uploading bitstreams"));

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        assertThat(bitstream.getResourcePolicies(), containsInAnyOrder(
            matches(READ, eperson, ResourcePolicy.TYPE_SUBMISSION),
            matches(WRITE, eperson, ResourcePolicy.TYPE_SUBMISSION),
            matches(ADD, eperson, ResourcePolicy.TYPE_SUBMISSION),
            matches(REMOVE, eperson, ResourcePolicy.TYPE_SUBMISSION),
            matches(DELETE, eperson, ResourcePolicy.TYPE_SUBMISSION),
            matches(READ, adminGroup, "administrator", ResourcePolicy.TYPE_CUSTOM, "custom admin policy"),
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2025-02-23", null, null)));

        assertThat(getItemBitstreams(item), hasSize(2));
    }

    @Test
    public void testUploadMultipleBitstreams() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-multiple-bitstreams-to-items.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), contains(
            containsString("Row 2 - Invalid item left in workspace"),
            containsString("Row 3 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 4 bitstreams to process"),
            is("Found 2 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Sheet bitstream-metadata - Row 3 - Bitstream created successfully"),
            containsString("Sheet bitstream-metadata - Row 4 - Bitstream created successfully"),
            containsString("Sheet bitstream-metadata - Row 5 - Bitstream created successfully")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));
        Item item2 = getItemFromMessage(handler.getWarningMessages().get(1));

        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE"), contains(
            bitstreamWith("Test title", "test file description", "this is a test file for uploading bitstreams")));

        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE2"), contains(
            bitstreamWith("Test title 2", "test file description 2",
                "this is a test file for uploading bitstreams"),
                bitstreamWith("Test title 3", "test file description 3",
                        "this is a second test file for uploading bitstreams")));

        assertThat(getItemBitstreams(item), hasSize(4));

        assertThat(getItemBitstreamsByBundle(item2, "SECOND-BUNDLE"), contains(
            bitstreamWith("Test title 4", "test file description 4",
                "this is a third test file for uploading bitstreams")));

        assertThat(getItemBitstreams(item2), hasSize(2));
    }

    @Test
    public void testUploadMultipleBitstreamWithPathTraversal() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-multiple-bitstreams-with-path-traversal-to-items.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), contains(
            "Access to the specified file file://../config/dspace.cfg is not allowed",
            "Cannot create bitstream from file at path file://../config/dspace.cfg",
            "Access to the specified file file:///home/ubuntu/.ssh/config is not allowed",
            "Cannot create bitstream from file at path file:///home/ubuntu/.ssh/config"));
        assertThat(handler.getWarningMessages(), contains(
            containsString("Row 2 - Invalid item left in workspace"),
            containsString("Row 3 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 3 bitstreams to process"),
            is("Found 2 items to process"),
            containsString("Sheet bitstream-metadata - Row 4 - Bitstream created successfully")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));
        Item item2 = getItemFromMessage(handler.getWarningMessages().get(1));

        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE"), empty());
        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE2"), empty());
        assertThat(getItemBitstreamsByBundle(item2, "SECOND-BUNDLE"), contains(
            bitstreamWith("Test title 3", "test file description 3",
                "this is a third test file for uploading bitstreams")));

    }

    @Test
    public void testUploadBitstreamWithRemoteFilePathNotFromAllowedIps() throws Exception {

        try {
            context.turnOffAuthorisationSystem();
            Collection publication = createCollection(context, community)
                    .withSubmissionDefinition("publication")
                    .withAdminGroup(eperson)
                    .build();
            context.commit();
            context.restoreAuthSystemState();

            configurationService.setProperty("allowed.ips.import", new String[]{"127.0.1.2"});

            String fileLocation = getXlsFilePath("add-bitstream-with-http-url-to-item.xls");

            String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
                    "-e", eperson.getEmail()};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

            assertThat(handler.getErrorMessages(), contains(
                    "Cannot create bitstream from file at path http://127.0.1.1"));
            assertThat(handler.getWarningMessages(), contains(
                    containsString("Domain '127.0.1.1' is not in the allowed list. Path: http://127.0.1.1"),
                    containsString("Row 2 - Invalid item left in workspace"),
                    containsString("Row 3 - Invalid item left in workspace")));
            assertThat(handler.getInfoMessages(), contains(
                    is("Start reading all the metadata group rows"),
                    is("Found 4 metadata groups to process"),
                    is("Start reading all the bitstream rows"),
                    is("Found 1 bitstreams to process"),
                    is("Found 2 items to process")));

            Item item = getItemFromMessage(handler.getWarningMessages().get(1));
            Item item2 = getItemFromMessage(handler.getWarningMessages().get(2));

            assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE"), empty());
            assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE2"), empty());
            assertThat(getItemBitstreamsByBundle(item2, "SECOND-BUNDLE"), empty());

        } finally {
            configurationService.setProperty("allowed.ips.import", new String[]{});
        }
    }

    @Test
    public void testUploadBitstreamWithRemoteFilePathFromAllowedIps() throws Exception {
        try {
            InputStream mockInputStream = new ByteArrayInputStream("mocked content".getBytes());

            context.turnOffAuthorisationSystem();
            Collection publication = createCollection(context, community)
                    .withSubmissionDefinition("publication")
                    .withAdminGroup(eperson)
                    .build();
            context.commit();
            context.restoreAuthSystemState();

            configurationService.setProperty("allowed.ips.import", new String[]{"127.0.1.1"});

            String fileLocation = getXlsFilePath("add-bitstream-with-http-url-to-item.xls");

            String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
                    "-e", eperson.getEmail()};
            TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

            ImportFileUtilMockClass importFileUtilSpy = spy(new ImportFileUtilMockClass(handler));
            doReturn(mockInputStream).when(importFileUtilSpy).openStream(any(URL.class));

            ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
            ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

            BulkImport script = null;
            if (scriptConfiguration != null) {
                script = (BulkImport) scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
                script.setImportFileUtil(importFileUtilSpy);
            }
            if (script != null) {
                if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, handler, eperson))) {
                    script.run();
                }
            }

            assertThat(handler.getErrorMessages(), empty());
            assertThat(handler.getWarningMessages(), contains(
                    containsString("Row 2 - Invalid item left in workspace"),
                    containsString("Row 3 - Invalid item left in workspace")));
            assertThat(handler.getInfoMessages(), contains(
                    is("Start reading all the metadata group rows"),
                    is("Found 4 metadata groups to process"),
                    is("Start reading all the bitstream rows"),
                    is("Found 1 bitstreams to process"),
                    is("Found 2 items to process"),
                    containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully")));

            Item item = getItemFromMessage(handler.getWarningMessages().get(0));

            assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE"), contains(
            bitstreamWith("Test title", "test file description",
                "mocked content")));

        } finally {
            configurationService.setProperty("allowed.ips.import", new String[]{});
        }
    }

    @Test
    public void testUploadBitstreamWithRemoteFilePathAndEmptyAllowedIps() throws Exception {

        InputStream mockInputStream = new ByteArrayInputStream("mocked content".getBytes());

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-bitstream-with-http-url-to-item.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        ImportFileUtilMockClass importFileUtilSpy = spy(new ImportFileUtilMockClass(handler));
        doReturn(mockInputStream).when(importFileUtilSpy).openStream(any(URL.class));

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        BulkImport script = null;
        if (scriptConfiguration != null) {
            script = (BulkImport) scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
            script.setImportFileUtil(importFileUtilSpy);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, handler, eperson))) {
                script.run();
            }
        }

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), contains(
            containsString("Row 2 - Invalid item left in workspace"),
            containsString("Row 3 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 2 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));

        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE"), contains(
            bitstreamWith("Test title", "test file description",
                          "mocked content")));

    }

    @Test
    public void testUploadMultipleBitstreamWithCorrectLocalPath() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-multiple-bitstreams-with-local-path.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
                "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), contains(
                containsString("Row 2 - Invalid item left in workspace"),
                containsString("Row 3 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
                is("Start reading all the metadata group rows"),
                is("Found 4 metadata groups to process"),
                is("Start reading all the bitstream rows"),
                is("Found 2 bitstreams to process"),
                is("Found 2 items to process"),
                containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
                containsString("Sheet bitstream-metadata - Row 3 - Bitstream created successfully")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));
        Item item2 = getItemFromMessage(handler.getWarningMessages().get(1));

        assertThat(getItemBitstreamsByBundle(item, "FIRST-BUNDLE"), contains(
                bitstreamWith("Test title 2", "test file description 2",
                        "this is a second test file for uploading bitstreams")));
        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE2"), empty());
        assertThat(getItemBitstreamsByBundle(item2, "SECOND-BUNDLE"), contains(
                bitstreamWith("Test title 3", "test file description 3",
                        "this is a third test file for uploading bitstreams")));

    }

    @Test
    public void testUploadMultipleBitstreamWithWrongLocalPath() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-multiple-bitstreams-with-wrong-local-path.xls");

        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
                "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), contains(
                "Access to the specified file file://../test_2.txt is not allowed",
                "Cannot create bitstream from file at path file://../test_2.txt",
                "Access to the specified file file:///bulk-uploads/test_2.txt is not allowed",
                "Cannot create bitstream from file at path file:///bulk-uploads/test_2.txt",
                "Access to the specified file file:///subfolder/test_2.txt is not allowed",
                "Cannot create bitstream from file at path file:///subfolder/test_2.txt"));
        assertThat(handler.getWarningMessages(), contains(
                containsString("Row 2 - Invalid item left in workspace"),
                containsString("Row 3 - Invalid item left in workspace")));
        assertThat(handler.getInfoMessages(), contains(
                is("Start reading all the metadata group rows"),
                is("Found 4 metadata groups to process"),
                is("Start reading all the bitstream rows"),
                is("Found 3 bitstreams to process"),
                is("Found 2 items to process")));

        Item item = getItemFromMessage(handler.getWarningMessages().get(0));
        Item item2 = getItemFromMessage(handler.getWarningMessages().get(1));

        assertThat(getItemBitstreamsByBundle(item, "FIRST-BUNDLE"), empty());
        assertThat(getItemBitstreamsByBundle(item, "TEST-BUNDLE2"), empty());
        assertThat(getItemBitstreamsByBundle(item2, "SECOND-BUNDLE"), empty());

    }

    @Test
    public void testUploadSingleBitstreamUpdate() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("54321")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("add-bitstream-to-item-update.xls");
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 2 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Row 2 - Item updated successfully")));

        assertThat(getItemBitstreamsByBundle(publicationItem, "TEST-BUNDLE"), contains(
            bitstreamWith("Test title", "test file description", "this is a test file for uploading bitstreams")));
    }

    @Test
    public void testUploadMultipleBitstreamsUpdateMultiple() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("54321")
            .build();

        Item publicationItem2 = createItem(context, publication)
            .withTitle("Test Publication 2")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("98765")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-multiple-items-update.xls";
        String fileLocation = getXlsFilePath(fileName);
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 2 bitstreams to process"),
            is("Found 2 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Row 2 - Item updated successfully"),
            containsString("Sheet bitstream-metadata - Row 3 - Bitstream created successfully"),
            containsString("Row 3 - Item updated successfully")));

        assertThat(getItemBitstreamsByBundle(publicationItem, "ORIGINAL"), contains(
            bitstreamWith("Test title", "test file description", "this is a test file for uploading bitstreams")));

        assertThat(getItemBitstreamsByBundle(publicationItem2, "TEST-BUNDLE2"), contains(
            bitstreamWith("Test title 2", "test file description 2",
                "this is a second test file for uploading bitstreams")));
    }

    @Test
    public void testUploadSingleBitstreamUpdateWithExistingBundle() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item publicationItem = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Luca G.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("54321")
            .build();

        bundleService.create(context, publicationItem, "JM-BUNDLE");

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "add-bitstream-to-item-bundle.xls";
        String fileLocation = getXlsFilePath(fileName);
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 2 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Row 2 - Item updated successfully")));

        // Assert that no new bundle was created from script
        assertThat(publicationItem.getBundles(), hasSize(1));

        assertThat(getItemBitstreamsByBundle(publicationItem, "JM-BUNDLE"), contains(
            bitstreamWith("Test title", "test file description", "this is a test file for uploading bitstreams")));
    }

    @Test
    public void testCreatePublicationInWorkspaceItemsAndItemHasLicense() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("items-with-bitstreams.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 2 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Row 2 - WorkflowItem created successfully")));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(handler.getInfoMessages().get(6));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));
        assertThat(findWorkspaceItem(createdItem), nullValue());

        List<Bitstream> licenses = getItemBitstreamsByBundle(createdItem, "LICENSE");
        assertThat(licenses, hasSize(1));
        assertThat(getBitstreamContent(licenses.get(0)), containsString("NOTE: PLACE YOUR OWN LICENSE HERE\n" +
            "This sample license is provided for informational purposes only."));

        assertThat(getItemBitstreamsByBundle(createdItem, "ORIGINAL"), contains(
            bitstreamWith("Test title.txt", "test file descr", "this is a test file for uploading bitstreams")));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "publication with attachment uploaded part second")));
        assertThat(metadata, hasItems(with("dc.title.alternative", "lorem ipsum new new new")));
        assertThat(metadata, hasItems(with("dc.date.issued", "2022-05-31")));
        assertThat(metadata, hasItems(with("dc.type", "Resource Types::text::manuscript")));
        assertThat(metadata, hasItems(with("dc.language.iso", "en")));
        assertThat(metadata, hasItems(with("dc.contributor.author",
            "Lombardi, Corrado", "b5ad6864-012d-4989-8e0d-4acfa1156fd9", 0, 600)));
        assertThat(metadata, hasItems(with("oairecerif.author.affiliation", "4Science",
            "a14ba215-c0f0-4b74-b21a-06359bfabd45", 0, 600)));
        assertThat(metadata, hasItems(with("dc.contributor.editor",
            "Corrado Francesco, Lombardi", "29177bec-ff50-4428-aa43-1fdf932f0d33", 0, 600)));

    }

    /**
     * Test Bitstream format of created Bitstreams.
     */
    @Test
    public void testCreatePublicationInWorkspaceItemsWithBitstreams() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "items-with-bitstreams.xlsx";
        String fileLocation = getXlsFilePath(fileName);
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 2 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Row 2 - WorkflowItem created successfully")));

        // verify created item (ROW 2)
        Item createdItem = getItemFromMessage(handler.getInfoMessages().get(6));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));
        assertThat(findWorkspaceItem(createdItem), nullValue());

        List<Bitstream> licenses = getItemBitstreamsByBundle(createdItem, "LICENSE");
        assertThat(licenses, hasSize(1));
        assertThat(getBitstreamContent(licenses.get(0)), containsString("NOTE: PLACE YOUR OWN LICENSE HERE\n" +
            "This sample license is provided for informational purposes only."));

        BitstreamFormat bf = licenses.get(0).getFormat(context);

        assertThat(bf.getMIMEType(), is("text/plain; charset=utf-8"));
        assertThat(bf.getShortDescription(), is("License"));
        assertThat(bf.getDescription(), is("Item-specific license agreed to upon submission"));

        List<Bitstream> bitstreams = getItemBitstreamsByBundle(createdItem, "ORIGINAL");
        assertThat(bitstreams, contains(
            bitstreamWith("Test title.txt", "test file descr", "this is a test file for uploading bitstreams")));

        BitstreamFormat bf1 = bitstreams.get(0).getFormat(context);

        assertThat(bf1.getMIMEType(), is("text/plain"));
        assertThat(bf1.getShortDescription(), is("Text"));
        assertThat(bf1.getDescription(), is("Plain Text"));

    }

    @Test
    public void testUpdateAndDeleteBitstreamsOfItems() throws Exception {

        context.turnOffAuthorisationSystem();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Collection publication = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

        Item publicationItem = createItem(context, publication)
                .withTitle("Test Publication")
                .withAuthor("Eskander M.")
                .withDescription("This is a test for bulk import")
                .withIsniIdentifier("54321")
                .build();

        Bitstream firstBitstream = createBitstream(context, publicationItem, toInputStream("TEST CONTENT", UTF_8))
            .withName("title")
            .withMimeType("text/plain")
            .build();

        Bitstream secondBitstream = createBitstream(context, publicationItem, toInputStream("TEST CONTENT", UTF_8))
            .withName("title 2")
            .withMimeType("text/plain")
            .build();

        Bitstream thirdBitstream = createBitstream(context, publicationItem, toInputStream("TEST CONTENT", UTF_8))
            .withName("title 3")
            .withMimeType("text/plain")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileName = "update-delete-bitstreams-of-items.xls";
        String fileLocation = getXlsFilePath(fileName);
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), containsInAnyOrder(
            "Sheet bitstream-metadata - Row 2 - Invalid ACCESS-CONDITION: [INAVALID_NAME]",
            "Sheet bitstream-metadata - Row 3 - The access condition embargo requires a start date.",
            "Sheet bitstream-metadata - Row 4 - The access condition embargo requires a start date."
        ));
        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 5 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 3 - Bitstream updated successfully"),
            containsString("Sheet bitstream-metadata - Row 4 - Bitstream updated successfully"),
            containsString("Sheet bitstream-metadata - Row 5 - Bitstream updated successfully"),
            containsString("Sheet bitstream-metadata - Row 6 - Bitstream deleted successfully"),
            containsString("Sheet bitstream-metadata - Row 7 - Bitstream updated successfully"),
            containsString("Row 2 - Item updated successfully")));

        publicationItem = context.reloadEntity(publicationItem);

        List<Bitstream> itemBitstreams = getItemBitstreams(publicationItem);
        assertThat(itemBitstreams, contains(firstBitstream, thirdBitstream));

        assertThat(context.reloadEntity(secondBitstream).isDeleted(), is(true));

        firstBitstream = context.reloadEntity(firstBitstream);
        assertThat(firstBitstream, bitstreamWith("Test title", "test file description", "TEST CONTENT"));

        assertThat(firstBitstream.getResourcePolicies(), containsInAnyOrder(
            matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM, "open access description"),
            matches(READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-02-01", "description here"),
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2023-01-12", null, "description here")));

        thirdBitstream = context.reloadEntity(thirdBitstream);
        assertThat(thirdBitstream, bitstreamWith("Test title 2", "description 2", "TEST CONTENT"));

    }


    @Test
    public void testBitstreamUpdateAndDeleteWithWrongPosition() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Eskander M.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("54321")
            .build();

        Bitstream bitstream = createBitstream(context, item, toInputStream("TEST CONTENT", UTF_8))
            .withName("Original bitstream title")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-delete-bitstreams-of-items.xls");
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        assertThat(handler.getErrorMessages(), containsInAnyOrder(
            is("Sheet bitstream-metadata - Row 2 - Invalid ACCESS-CONDITION: [INAVALID_NAME]"),
            is("Sheet bitstream-metadata - Row 3 - The access condition embargo requires a start date."),
            is("Sheet bitstream-metadata - Row 4 - The access condition embargo requires a start date."),
            containsString("Sheet bitstream-metadata - Row 6 - No bitstream found at position 2 for Item with id"),
            containsString("Sheet bitstream-metadata - Row 7 - No bitstream found at position 3 for Item with id")));

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 4 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 5 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 3 - Bitstream updated successfully"),
            containsString("Sheet bitstream-metadata - Row 4 - Bitstream updated successfully"),
            containsString("Sheet bitstream-metadata - Row 5 - Bitstream updated successfully"),
            containsString("Row 2 - Item updated successfully")));

        item = context.reloadEntity(item);

        bitstream = context.reloadEntity(bitstream);
        assertThat(bitstream, bitstreamWith("Test title", "test file description", "TEST CONTENT"));
    }

    @Test
    public void testBitstreamUpdateWithAdditionalConditionSetToFalse() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection publication = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = createItem(context, publication)
            .withTitle("Test Publication")
            .withAuthor("Eskander M.")
            .withDescription("This is a test for bulk import")
            .withIsniIdentifier("54321")
            .build();

        Bitstream bitstream = createBitstream(context, item, toInputStream("TEST CONTENT", UTF_8))
            .withName("Original bitstream title")
            .build();

        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(bitstream)
            .withName("test")
            .withAction(READ)
            .withPolicyType(TYPE_CUSTOM)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-bitstream-policies-without-additional-ac.xls");
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat("Expected no warnings", handler.getWarningMessages(), empty());

        assertThat(handler.getErrorMessages(), empty());

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 0 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 1 bitstreams to process"),
            is("Found 1 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream updated successfully"),
            containsString("Row 2 - Item updated successfully")));

        item = context.reloadEntity(item);

        bitstream = context.reloadEntity(bitstream);
        assertThat(bitstream, bitstreamWith("Test title", "test file description", "TEST CONTENT"));

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        assertThat(bitstream.getResourcePolicies(), containsInAnyOrder(
            matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM, "open access description"),
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2023-01-12", null, null)));
    }

    @Test
    public void testUpdateItems() throws Exception {
        String oldDescription = "This is a test";
        String newDescription = "Lorem ipsum";
        // prepare data
        context.turnOffAuthorisationSystem();
        Collection publication = createCollection(context, community)
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();
        Item publication1 = createItem(context, publication)
                .withTitle("Test Publication 1")
                .withAuthor("Scognamiglio, Francesco Pio")
                .withDescription(oldDescription)
                .withIsniIdentifier("12345")
                .build();
        Item publication2 = createItem(context, publication)
                .withTitle("Test Publication 2")
                .withAuthor("Scognamiglio, Francesco Pio")
                .withDescription(oldDescription)
                .withIsniIdentifier("12346")
                .build();
        Item publication3 = createItem(context, publication)
                .withTitle("Test Publication 3")
                .withAuthor("Scognamiglio, Francesco Pio")
                .withDescription(oldDescription)
                .withIsniIdentifier("12347")
                .build();
        context.commit();
        context.restoreAuthSystemState();

        // start test
        String fileLocation = getXlsFilePath("update-items.xls");
        String[] args = new String[] { "bulk-import", "-c", publication.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(6));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 3 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(5), containsString("Row 4 - Item updated successfully"));

        assertSearchQuery(IndexableItem.TYPE, oldDescription, 0);
        assertSearchQuery(IndexableItem.TYPE, newDescription, 3);
    }

    private void assertSearchQuery(String resourceType, String description, int size) throws SearchServiceException {
        assertSearchQuery(resourceType, description, size, size, 0, -1);
    }

    private void assertSearchQuery(String resourceType, String description,
            int size, int totalFound, int start, int limit)
        throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setQuery("*:*");
        discoverQuery.setStart(start);
        discoverQuery.setMaxResults(limit);
        discoverQuery.addFilterQueries("search.resourcetype:" + resourceType);
        discoverQuery.addFilterQueries("dc.description:\"" + description + "\"");
        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();
        assertEquals(size, indexableObjects.size());
        assertEquals(totalFound, discoverResult.getTotalSearchResults());
    }

    @Test
    public void testCreatePublicationWithSecurityLevel() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-publication-with-security-level.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item archived successfully"));

        Item createdItem = getItemFromMessage(infoMessages.get(3));
        assertThat(createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(true));

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test Publication")));
        assertThat(metadata, hasItems(withSecurity("dc.type", "Article", 1)));
        assertThat(metadata, hasItems(withSecurity("dc.relation.publication", "First publication",
            "authority1", 0, 600, 2)));
        assertThat(metadata, hasItems(with("dc.relation.publication", "Second publication", "authority2", 1, 600)));
        assertThat(metadata, hasItems(withSecurity("dc.relation.publication", "Third publication",
            "authority3", 2, 400, 0)));

    }

    @Test
    public void testUpdatePublicationWithSecurityLevel() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item item = ItemBuilder.createItem(context, publications)
            .withTitle("My Item")
            .withIssueDate("2020-01-01")
            .withSecuredMetadata("dc", "type", null, "Article", 2)
            .withDoiIdentifier("123456")
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("update-publication-with-security-level.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(4));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));

        item = context.reloadEntity(item);
        assertThat(item, notNullValue());

        List<MetadataValue> metadata = item.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Test Publication")));
        assertThat(metadata, hasItems(withSecurity("dc.type", "Article", 1)));
        assertThat(metadata, hasItems(withSecurity("dc.relation.publication", "First publication",
            "authority1", 0, 600, 2)));
        assertThat(metadata, hasItems(with("dc.relation.publication", "Second publication", "authority2", 1, 600)));
        assertThat(metadata, hasItems(withSecurity("dc.relation.publication", "Third publication",
            "authority3", 2, 400, 0)));

    }

    @Test
    public void testWorkbookWithoutActionColumn() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(eperson)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("without-action-column.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat("Expected 1 warning message", warningMessages, hasSize(1));
        assertThat(warningMessages.get(0), containsString("Row 2 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat("Expected 3 info messages", infoMessages, hasSize(3));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 1 items to process"));

        Item createdItem = getItemFromMessage(warningMessages.get(0));
        assertThat("Item expected to be created", createdItem, notNullValue());
        assertThat(createdItem.isArchived(), is(false));
        assertThat(findWorkspaceItem(createdItem), notNullValue());

        List<MetadataValue> metadata = createdItem.getMetadata();
        assertThat(metadata, hasItems(with("dc.title", "Patent")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Tom Jones")));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Luca Stone", 1)));
        assertThat(metadata, hasItems(with("dc.contributor.author", "Edward Red", 2)));
        assertThat(metadata, hasItems(with("dc.publisher", "Publisher")));

    }

    @Test
    public void testWorkbookWithDiscoverableColumn() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        Item firstPublication = ItemBuilder.createItem(context, publications)
            .withTitle("First Publication")
            .withDoiIdentifier("123456")
            .makeUnDiscoverable()
            .build();

        Item secondPublication = ItemBuilder.createItem(context, publications)
            .withTitle("Second Publication")
            .withDoiIdentifier("987654")
            .build();

        Item thirdPublication = ItemBuilder.createItem(context, publications)
            .withTitle("Third Publication")
            .withDoiIdentifier("111222")
            .makeUnDiscoverable()
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("publications_with_discoverable_column.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat("Expected no errors", handler.getErrorMessages(), empty());

        List<String> warningMessages = handler.getWarningMessages();
        assertThat(warningMessages, hasSize(3));
        assertThat(warningMessages.get(0), containsString("Row 5 - Invalid item left in workspace"));
        assertThat(warningMessages.get(1), containsString("Row 6 - Invalid item left in workspace"));
        assertThat(warningMessages.get(2), containsString("Row 7 - Invalid item left in workspace"));

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(6));
        assertThat(infoMessages.get(0), containsString("Start reading all the metadata group rows"));
        assertThat(infoMessages.get(1), containsString("Found 0 metadata groups to process"));
        assertThat(infoMessages.get(2), containsString("Found 6 items to process"));
        assertThat(infoMessages.get(3), containsString("Row 2 - Item updated successfully"));
        assertThat(infoMessages.get(4), containsString("Row 3 - Item updated successfully"));
        assertThat(infoMessages.get(5), containsString("Row 4 - Item updated successfully"));

        firstPublication = context.reloadEntity(firstPublication);
        assertThat(firstPublication.isDiscoverable(), is(true));

        secondPublication = context.reloadEntity(secondPublication);
        assertThat(secondPublication.isDiscoverable(), is(false));

        thirdPublication = context.reloadEntity(thirdPublication);
        assertThat(thirdPublication.isDiscoverable(), is(false));

        Item fourthPublication = getItemFromMessage(warningMessages.get(0));
        assertThat(fourthPublication.isDiscoverable(), is(true));

        Item fifthPublication = getItemFromMessage(warningMessages.get(1));
        assertThat(fifthPublication.isDiscoverable(), is(false));

        Item sixthPublication = getItemFromMessage(warningMessages.get(2));
        assertThat(sixthPublication.isDiscoverable(), is(true));

    }

    @Test
    public void testWorkbookWithInvalidOptionalColumnPosition() throws Exception {

        context.turnOffAuthorisationSystem();

        Collection publications = createCollection(context, community)
            .withSubmissionDefinition("publication")
            .withAdminGroup(eperson)
            .build();

        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("invalid-optional-column-position.xlsx");
        String[] args = new String[] { "bulk-import", "-c", publications.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(),
            contains("BulkImportException: The optional column DISCOVERABLE present in sheet Main "
                + "must be placed before the metadata fields"));
    }

    @Test
    public void testCreatePatentByNotCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection patents = createCollection(context, community)
            .withSubmissionDefinition("patent")
            .withAdminGroup(admin)
            .build();
        context.commit();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("create-patent.xls");
        String[] args = new String[] { "bulk-import", "-c", patents.getID().toString(), "-f", fileLocation,
            "-e", eperson.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat("Expected 1 error message", errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The user is not an admin of the given collection"));
    }

    private WorkspaceItem findWorkspaceItem(Item item) throws SQLException {
        return workspaceItemService.findByItem(context, item);
    }

    private Item findItemByMetadata(String schema, String element, String qualifier, String value) throws Exception {
        Iterator<Item> iterator = itemService.findArchivedByMetadataField(context, schema, element, qualifier, value);
        return iterator.hasNext() ? iterator.next() : null;
    }

    private Item getItemFromMessage(String message) throws SQLException {
        Matcher matcher = UUID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String uuid = matcher.group(0);
        return itemService.find(context, UUID.fromString(uuid));
    }

    private String getXlsFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }

    private List<Bitstream> getItemBitstreamsByBundle(Item item, String bundleName) {
        try {
            return itemService.getBundles(context.reloadEntity(item), bundleName).stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Bitstream> getItemBitstreams(Item item) {
        try {
            return context.reloadEntity(item).getBundles().stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private org.hamcrest.Matcher<Bitstream> bitstreamWith(String title, String description, String content) {
        return both(hasContent(content))
            .and(hasTitleAndDescription(title, description));
    }

    private org.hamcrest.Matcher<Bitstream> hasContent(String content) {
        return matches(bitstream -> content.equals(getBitstreamContent(bitstream)),
            "Expected bitstream with content " + content);
    }

    private org.hamcrest.Matcher<? super Bitstream> hasTitleAndDescription(String title, String description) {
        return DSpaceObjectMatcher.withMetadata(containsInAnyOrder(
            with("dc.title", title), with("dc.description", description)));
    }

    private String getBitstreamContent(Bitstream bitstream) {
        try {
            InputStream inputStream = bitstreamService.retrieve(context, bitstream);
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

}
