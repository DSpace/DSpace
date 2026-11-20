/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.script;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link BulkItemExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class BulkItemExportIT extends AbstractIntegrationTestWithDatabase {

    private Community community;

    private Collection collection;

    @Before
    @SuppressWarnings("deprecation")
    public void beforeTests() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testBulkItemExport() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "My publication", "", "Publication");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Walter White</preferred-name>"));
            assertThat(content, containsString("<preferred-name>John Smith</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>My publication</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithQuery() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "Company", "", "OrgUnit");
        createItem(collection, "Edward Smith", "Science", "Person");
        createItem(collection, "John Smith", "Software", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-q", "Edward",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Edward Smith</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>Company</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithSingleFilter() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "My publication", "", "Publication");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Software", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-sf", "subject=Science", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Walter White</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>My publication</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithManyFilters() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "My publication", "", "Publication");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Software", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-sf", "subject=Science&title=Walter White", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 1 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Walter White</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>Edward Red</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>My publication</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithScope() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection anotherCollection = createCollection(context, community).withAdminGroup(eperson).build();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "My project", "", "Project");
        createItem(anotherCollection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Software", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-s", collection.getID().toString(), "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>John Smith</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>Walter White</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>My project</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithConfiguration() throws Exception {

        context.turnOffAuthorisationSystem();

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
        ChoiceAuthorityService choiceAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
        MetadataAuthorityService metadataAuthorityService =
            ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

        configurationService.setProperty("plugin.named.org.dspace.content.authority.ChoiceAuthority",
                                         new String[] {
                                             "org.dspace.content.authority.ItemAuthority = OrgUnitAuthority",
                                             "org.dspace.content.authority.ItemAuthority = AuthorAuthority"
                                         });
        configurationService.setProperty("cris.ItemAuthority.OrgUnitAuthority.entityType", "OrgUnit");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("choices.plugin.person.affiliation.name", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.person.affiliation.name", "suggest");
        configurationService.setProperty("authority.controlled.person.affiliation.name", "true");
        configurationService.setProperty("choices.plugin.oairecerif.person.affiliation", "OrgUnitAuthority");
        configurationService.setProperty("choices.presentation.oairecerif.person.affiliation", "suggest");
        configurationService.setProperty("authority.controlled.oairecerif.person.affiliation", "true");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.getChoiceAuthoritiesNames();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withTitle("4Science")
            .build();

        String orgUnitId = orgUnit.getID().toString();

        ItemBuilder.createItem(context, collection)
            .withTitle("Edward Red")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science", orgUnitId)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withEntityType("Person")
            .withPersonMainAffiliation("4Science", orgUnitId)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withEntityType("Person")
            .withPersonMainAffiliation("Company")
            .build();

        createItem(collection, "My project", "", "Project");

        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-s", orgUnitId, "-c", "RELATION.OrgUnit.people", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>John Smith</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>Walter White</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>My project</preferred-name>")));
        }

    }

    @Test
    public void testBulkItemExportWithQueryAndScope() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection anotherCollection = createCollection(context, community).withAdminGroup(eperson).build();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "Edward Mason", "Software", "Person");
        createItem(collection, "My publication", "", "Publication");
        createItem(anotherCollection, "Edward White", "Science", "Person");
        createItem(collection, "John Smith", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-s", collection.getID().toString(), "-sf", "subject=Science", "-q", "Edward", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 1 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>Edward Mason</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>Edward White</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>My publication</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithSortAscending() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-so", "dc.title,ASC",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Walter White</preferred-name>"));
            assertThat(content, containsString("<preferred-name>John Smith</preferred-name>"));
            assertThat(content.indexOf("Edward Red"), lessThan(content.indexOf("John Smith")));
            assertThat(content.indexOf("John Smith"), lessThan(content.indexOf("Walter White")));
        }
    }

    @Test
    public void testBulkItemExportWithSortDescending() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-so", "dc.title,DESC",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Walter White</preferred-name>"));
            assertThat(content, containsString("<preferred-name>John Smith</preferred-name>"));
            assertThat(content.indexOf("Walter White"), lessThan(content.indexOf("John Smith")));
            assertThat(content.indexOf("John Smith"), lessThan(content.indexOf("Edward Red")));
        }
    }

    @Test
    public void testBulkItemExportWithoutExportFormat() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The export format must be provided"));
    }

    @Test
    public void testBulkItemExportWithInvalidFormat() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "invalid", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No dissemination configured for format invalid"));
    }

    @Test
    public void testBulkItemExportWithInvalidFilter() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-sf", "testFilter",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Invalid filter: testFilter"));
    }

    @Test
    public void testBulkItemExportWithoutEntityType() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-f", "person-xml", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), contains("Found 0 items to export",
            "Items exported successfully into file named person.xml"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testBulkItemExportWithWorkspaceAndWorkflowItemsAndDefaultConfiguration() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection anotherCollection = createCollection(context, community).withAdminGroup(eperson).
                                                                           withWorkflowGroup(1, eperson).build();
        createItem(anotherCollection, "Edward Red", "Science", "Person");
        createItem(anotherCollection, "Company", "", "OrgUnit");
        createWorkspaceItem(anotherCollection, "Edward Smith", "Science", "Person");
        createItem(anotherCollection, "John Smith", "Software", "Person");
        createWorkflowItem(anotherCollection, "Edward White", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-q", "Edward",
            "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 1 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>Edward Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>Edward White</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>Company</preferred-name>")));
        }
    }

    @Test
    public void testBulkItemExportWithWorkspaceAndWorkflowItemsAndWorkspaceConfiguration() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "Company", "", "OrgUnit");
        createWorkspaceItem(collection, "Edward Smith", "Science", "Person");
        createItem(collection, "John Smith", "Software", "Person");
        createWorkflowItem(collection, "Edward White", "Science", "Person");
        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-q", "Edward", "-c", "workspace", "-e", eperson.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward Red</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Edward Smith</preferred-name>"));
            assertThat(content, containsString("<preferred-name>Edward White</preferred-name>"));
            assertThat(content, not(containsString("<preferred-name>John Smith</preferred-name>")));
            assertThat(content, not(containsString("<preferred-name>Company</preferred-name>")));
        }
    }

    @Test
    public void testSelectedItemsBulkItemExport() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item1 = createItem(collection, "Edward Red", "Science", "Publication");
        createItem(collection, "My publication", "", "Publication");
        Item item2 = createItem(collection, "Walter White", "Science", "Publication");
        createItem(collection, "John Smith", "Science", "Publication");
        context.restoreAuthSystemState();
        context.commit();

        String items = item1.getID().toString() + ";" + item2.getID().toString();

        String[] args = new String[] { "bulk-item-export", "-si", items, "-f", "publication-chicago",
            "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        File txt = new File("publications.txt");
        txt.deleteOnExit();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        try (FileInputStream fis = new FileInputStream(txt)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content,
                    containsString("“Edward Red,” n.d. http://localhost:4000/handle/" + item1.getHandle()));
            assertThat(content,
                    containsString("“Walter White,” n.d. http://localhost:4000/handle/" + item2.getHandle()));
        }
    }

    @Test
    public void testBulkItemExportChicagoWithMissingMetadata() throws Exception {

        context.turnOffAuthorisationSystem();
        Collection anotherCollection = createCollection(context, community)
            .withAdminGroup(eperson).build();

        // Create an archived Publication item with JUST entityType — no title, author,
        // date, or URI
        Item itemNoMetadata = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .build();

        // Create a workspace Publication item with no metadata
        WorkspaceItem workspaceItem = WorkspaceItemBuilder
            .createWorkspaceItem(context, anotherCollection)
            .withEntityType("Publication")
            .build();

        // Create a workflow Publication item with no metadata
        WorkflowItem workflowItem = WorkflowItemBuilder
            .createWorkflowItem(context, anotherCollection)
            .withEntityType("Publication")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        String items = itemNoMetadata.getID().toString() + ";"
            + workspaceItem.getItem().getID().toString() + ";"
            + workflowItem.getItem().getID().toString();

        String[] args = new String[] { "bulk-item-export", "-si", items, "-f", "publication-chicago",
            "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        File txt = new File("publications.txt");
        txt.deleteOnExit();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat("No errors should occur when exporting items with missing metadata",
            handler.getErrorMessages(), empty());
        assertThat("The output file should be created", txt.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(txt)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat("Output should contain a row for the archived item with n.d.",
                content, containsString("n.d."));
        }
    }

    @Test
    public void testBulkItemExportLimited() throws Exception {

        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createItem(collection, "My publication", "", "Publication");
        createItem(collection, "Walter White", "Science", "Person");
        createItem(collection, "John Smith", "Science", "Person");
        EPerson member = createEPerson();
        context.restoreAuthSystemState();
        context.commit();

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        int loggedInLimit = configurationService.getIntProperty("bulk-export.limit.loggedIn");
        int notLoggedInLimit = configurationService.getIntProperty("bulk-export.limit.notLoggedIn");
        configurationService.setProperty("bulk-export.limit.loggedIn", 2);
        configurationService.setProperty("bulk-export.limit.notLoggedIn", 1);

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] adminArgs = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-so", "dc.title,ASC", "-e", admin.getEmail() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(adminArgs, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        // eperson is the collection admin, so the output will be the same as the admin.
        String[] epersonArgs = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-so", "dc.title,ASC", "-e", eperson.getEmail() };
        handleScript(epersonArgs, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        // member is a newly created eperson, so it will be treated as a generic logged user.
        String[] memberArgs = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-so", "dc.title,ASC", "-e", member.getEmail() };
        handleScript(memberArgs, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Export will be limited to 2 items."));
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        // null eperson, so it will be treated as there is no user logged in.
        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-so", "dc.title,ASC" };
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Export will be limited to 1 items."));
        assertThat(handler.getInfoMessages(), hasItem("Found 1 items to export"));
        assertThat("The xml file should be created", xml.exists(), is(true));

        configurationService.setProperty("bulk-export.limit.loggedIn", loggedInLimit);
        configurationService.setProperty("bulk-export.limit.notLoggedIn", notLoggedInLimit);
    }

    @Test
    public void bulkItemExportWithDateIssuedAndLeftLimitTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createPublication("Title Publication1 2025", "subject one", "2025");
        createPublication("Test Publication bulk-import", "subject2", "2025-03-15");
        createPublication("Title Publication 2024", "subject33", "2024");
        createPublication("TitlePublication", "subject4", "2024-11-21");
        createPublication("Tets 2023", "subject", "2023");
        createPublication("Title Publication 4sciecnce", "subject99", "2023-06-20");
        context.restoreAuthSystemState();
        context.commit();

        File csv = new File("publications.csv");
        csv.deleteOnExit();

        String[] args = new String[] { "bulk-item-export",
                                       "-t", "Publication",
                                       "-f", "publication-csv",
                                       "-sf", "dateIssued=[2025 TO *],equals",
                                       "-c", "site",
                                       "-so", "score,DESC",
                                       "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", csv.exists(), is(true));

        try (FileInputStream file = new FileInputStream(csv)) {
            String content = IOUtils.toString(file, Charset.defaultCharset());
            // check that only 2025 publications was exported
            assertThat(content, containsString("\"Test Publication bulk-import\",\"\",\"\",\"\",\"2025-03-15\""));
            assertThat(content, containsString("\"Title Publication1 2025\",\"\",\"\",\"\",\"2025\""));
            // check that others publications wasn't exported
            assertThat(content, not(containsString("\"Title Publication 2024\"")));
            assertThat(content, not(containsString("\"TitlePublication\"")));
            assertThat(content, not(containsString("\"Tets 2023\"")));
            assertThat(content, not(containsString("\"Title Publication 4sciecnce\"")));
        }
    }

    @Test
    public void bulkItemExportWithDateIssuedAndLeftLimitWithoutOperatorEqualsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createPublication("Title Publication1 2025", "subject one", "2025");
        createPublication("Test Publication bulk-import", "subject2", "2025-03-15");
        createPublication("Title Publication 2024", "subject33", "2024");
        createPublication("TitlePublication", "subject4", "2024-11-21");
        createPublication("Tets 2023", "subject", "2023");
        createPublication("Title Publication 4sciecnce", "subject99", "2023-06-20");
        context.restoreAuthSystemState();
        context.commit();

        File csv = new File("publications.csv");
        csv.deleteOnExit();

        String[] args = new String[] { "bulk-item-export",
                                       "-t", "Publication",
                                       "-f", "publication-csv",
                                       "-sf", "dateIssued=[2025 TO *]",
                                       "-c", "site",
                                       "-so", "score,DESC",
                                       "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 3 items to export"));
        assertThat("The xml file should be created", csv.exists(), is(true));

        try (FileInputStream file = new FileInputStream(csv)) {
            String content = IOUtils.toString(file, Charset.defaultCharset());
            assertThat(content, containsString("\"Test Publication bulk-import\",\"\",\"\",\"\",\"2025-03-15\""));
            assertThat(content, containsString("\"Title Publication1 2025\",\"\",\"\",\"\",\"2025\""));
            assertThat(content, containsString("\"TitlePublication\",\"\",\"\",\"\",\"2024-11-21\""));

            assertThat(content, not(containsString("\"Title Publication 2024\"")));
            assertThat(content, not(containsString("\"Tets 2023\"")));
            assertThat(content, not(containsString("\"Title Publication 4sciecnce\"")));
        }
    }

    @Test
    public void bulkItemExportWithDateIssuedAndRightLimitTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createPublication("Title Publication1 2025", "subject one", "2025");
        createPublication("Test Publication bulk-import", "subject2", "2025-03-15");
        createPublication("Title Publication 2024", "subject33", "2024");
        createPublication("TitlePublication", "subject4", "2024-11-21");
        createPublication("Tets 2023", "subject", "2023");
        createPublication("Title Publication 4sciecnce", "subject99", "2023-06-20");
        context.restoreAuthSystemState();
        context.commit();

        File csv = new File("publications.csv");
        csv.deleteOnExit();

        String[] args = new String[] { "bulk-item-export",
                                       "-t", "Publication",
                                       "-f", "publication-csv",
                                       "-sf", "dateIssued=[* TO 2024],equals",
                                       "-c", "site",
                                       "-so", "score,DESC",
                                       "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 4 items to export"));
        assertThat("The xml file should be created", csv.exists(), is(true));

        try (FileInputStream file = new FileInputStream(csv)) {
            String content = IOUtils.toString(file, Charset.defaultCharset());
            // check that only until 2024 publications was exported
            assertThat(content, containsString("\"Title Publication 2024\",\"\",\"\",\"\",\"2024\""));
            assertThat(content, containsString("\"TitlePublication\",\"\",\"\",\"\",\"2024-11-21\""));
            assertThat(content, containsString("\"Tets 2023\",\"\",\"\",\"\",\"2023\""));
            assertThat(content, containsString("\"Title Publication 4sciecnce\",\"\",\"\",\"\",\"2023-06-20\""));
            // check that others publications wasn't exported
            assertThat(content, not(containsString("\"Test Publication bulk-import\"")));
            assertThat(content, not(containsString("\"Title Publication1 2025\"")));
        }
    }

    @Test
    public void bulkItemExportWithDateIssuedAndOnlySpecificYearTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createItem(collection, "Edward Red", "Science", "Person");
        createPublication("Title Publication1 2025", "subject one", "2025");
        createPublication("Test Publication bulk-import", "subject2", "2025-03-15");
        createPublication("Title Publication 2024", "subject33", "2024");
        createPublication("TitlePublication", "subject4", "2024-11-21");
        createPublication("Tets 2023", "subject", "2023");
        createPublication("Title Publication 4sciecnce", "subject99", "2023-06-20");
        context.restoreAuthSystemState();
        context.commit();

        File csv = new File("publications.csv");
        csv.deleteOnExit();

        String[] args = new String[] { "bulk-item-export",
                                       "-t", "Publication",
                                       "-f", "publication-csv",
                                       "-sf", "dateIssued=[2024 TO 2024],equals",
                                       "-c", "site",
                                       "-so", "score,DESC",
                                       "-e", eperson.getEmail() };

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, null);

        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getInfoMessages(), hasItem("Found 2 items to export"));
        assertThat("The xml file should be created", csv.exists(), is(true));

        try (FileInputStream file = new FileInputStream(csv)) {
            String content = IOUtils.toString(file, Charset.defaultCharset());
            // check that only 2024 publications was exported
            assertThat(content, containsString("\"Title Publication 2024\",\"\",\"\",\"\",\"2024\""));
            assertThat(content, containsString("\"TitlePublication\",\"\",\"\",\"\",\"2024-11-21\""));

            // check that others publications wasn't exported
            assertThat(content, not(containsString("\"Test Publication bulk-import\"")));
            assertThat(content, not(containsString("\"Title Publication1 2025\"")));
            assertThat(content, not(containsString("\"Tets 2023\"")));
            assertThat(content, not(containsString("\"Title Publication 4sciecnce\"")));
        }
    }

    private Item createItem(Collection collection, String title, String subject, String entityType) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withSubject(subject)
            .withEntityType(entityType)
            .build();
    }

    private Item createPublication(String title, String subject, String dateIssued) {
        return ItemBuilder.createItem(context, collection)
                          .withTitle(title)
                          .withSubject(subject)
                          .withIssueDate(dateIssued)
                          .withEntityType("Publication")
                          .build();
    }

    private EPerson createEPerson() {
        return EPersonBuilder.createEPerson(context).withEmail("member@email.com").build();
    }

    private WorkspaceItem createWorkspaceItem(Collection collection, String title, String subject, String entityType) {
        return WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle(title)
            .withSubject(subject)
            .withEntityType(entityType)
            .build();
    }

    private WorkflowItem createWorkflowItem(Collection collection, String title, String subject, String entityType) {
        return WorkflowItemBuilder.createWorkflowItem(context, collection)
            .withTitle(title)
            .withSubject(subject)
            .withEntityType(entityType)
            .build();
    }
}
