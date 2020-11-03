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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

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
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration tests for {@link BulkItemExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Ignore
public class BulkItemExportIT extends AbstractIntegrationTestWithDatabase {

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

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-q", "Edward" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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
            "-sf", "subject=Science" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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
            "-sf", "subject=Science&title=Walter White" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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
            "-s", collection.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withTitle("4Science")
            .build();

        String orgUnitId = orgUnit.getID().toString();

        ItemBuilder.createItem(context, collection)
            .withTitle("Edward Red")
            .withRelationshipType("Person")
            .withPersonMainAffiliation("4Science", orgUnitId)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("John Smith")
            .withRelationshipType("Person")
            .withPersonMainAffiliation("4Science", orgUnitId)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Walter White")
            .withRelationshipType("Person")
            .withPersonMainAffiliation("Company")
            .build();

        createItem(collection, "My project", "", "Project");

        context.restoreAuthSystemState();
        context.commit();

        File xml = new File("person.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml",
            "-s", orgUnitId, "-c", "RELATION.OrgUnit.people" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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
            "-s", collection.getID().toString(), "-sf", "subject=Science", "-q", "Edward" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-so", "dc.title,ASC" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-so", "dc.title,DESC" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

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

        String[] args = new String[] { "bulk-item-export", "-t", "Person" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The export format must be provided"));
    }

    @Test
    public void testBulkItemExportWithInvalidFormat() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "invalid" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No dissemination configured for format invalid"));
    }

    @Test
    public void testBulkItemExportWithInvalidFilter() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-t", "Person", "-f", "person-xml", "-sf", "testFilter" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("Invalid filter: testFilter"));
    }

    @Test
    public void testBulkItemExportWithoutEntityType() throws Exception {

        String[] args = new String[] { "bulk-item-export", "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The entity type must be provided"));
    }

    private Item createItem(Collection collection, String title, String subject, String entityType) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withSubject(subject)
            .withRelationshipType(entityType)
            .build();
    }
}
