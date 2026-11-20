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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
import org.junit.Test;

/**
 * Integration tests for {@link ItemExport}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemExportIT extends AbstractIntegrationTestWithDatabase {

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
    public void testItemExport() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Edward White")
            .withJobTitle("Researcher")
            .withOrcidIdentifier("0000-0002-9077-5939")
            .build();
        context.restoreAuthSystemState();

        File xml = new File(System.getProperty("java.io.tmpdir"), "item-export-test.xml");
        xml.deleteOnExit();

        String[] args = new String[] { "item-export", "-i", item.getID().toString(),
            "-n", xml.getAbsolutePath(),
            "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());
        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward White</preferred-name>"));
            assertThat(content, containsString("<job-title>Researcher</job-title>"));
            assertThat(content, containsString("<orcid>0000-0002-9077-5939</orcid>"));
        }
    }

    @Test
    public void testItemExportWithoutFileName() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Edward White")
            .withJobTitle("Researcher")
            .withOrcidIdentifier("0000-0002-9077-5939")
            .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "item-export", "-i", item.getID().toString(), "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        assertThat(handler.getErrorMessages(), empty());

        File xml = new File("person.xml"); // default file name for xml person export
        xml.deleteOnExit();

        assertThat("The xml file should be created", xml.exists(), is(true));

        try (FileInputStream fis = new FileInputStream(xml)) {
            String content = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(content, containsString("<preferred-name>Edward White</preferred-name>"));
            assertThat(content, containsString("<job-title>Researcher</job-title>"));
            assertThat(content, containsString("<orcid>0000-0002-9077-5939</orcid>"));
        }

    }

    @Test
    public void testItemExportWithoutItemId() throws Exception {

        String[] args = new String[] { "item-export", "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat( errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("A valid item uuid should be provided"));

    }

    @Test
    public void testItemExportWithInvalidItemId() throws Exception {

        String[] args = new String[] { "item-export", "-i", "invalid", "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("A valid item uuid should be provided"));

    }

    @Test
    public void testItemExportWithoutFormat() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Edward White")
            .withJobTitle("Researcher")
            .withOrcidIdentifier("0000-0002-9077-5939")
            .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "item-export", "-i", item.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("The export format must be provided"));

    }

    @Test
    public void testItemExportWithInvalidFormat() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Edward White")
            .withJobTitle("Researcher")
            .withOrcidIdentifier("0000-0002-9077-5939")
            .build();
        context.restoreAuthSystemState();

        String[] args = new String[] { "item-export", "-i", item.getID().toString(), "-f", "invalid" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No dissemination configured for format invalid"));

    }

    @Test
    public void testItemExportWithNoItemFound() throws Exception {

        String itemId = "7b7b9082-39db-498d-a6dd-4a9f429d535b";
        String[] args = new String[] { "item-export", "-i", itemId, "-f", "person-xml" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(1));
        assertThat(errorMessages.get(0), containsString("No item found by id 7b7b9082-39db-498d-a6dd-4a9f429d535b"));
    }
}
