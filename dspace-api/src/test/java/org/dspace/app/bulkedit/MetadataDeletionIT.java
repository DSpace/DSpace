/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link MetadataDeletion}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataDeletionIT extends AbstractIntegrationTestWithDatabase {

    private MetadataValueService metadataValueService = ContentServiceFactory.getInstance().getMetadataValueService();

    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @BeforeEach
    public void setup() {

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        createItem(collection, "My First publication", "Mario Rossi");
        createItem(collection, "Another publication", "John Smith");

        context.restoreAuthSystemState();
    }

    @Test
    public void metadataDeletionListTest() throws Exception {

        configurationService.setProperty("bulkedit.allow-bulk-deletion", new String[] { "dc.title", "dc.type" });

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-l" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> infoMessages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(infoMessages, hasSize(1));
        assertThat(infoMessages, hasItem(equalTo("The fields that can be bulk deleted are: dc.title, dc.type")));
    }

    @Test
    public void metadataDeletionListWithoutErasableMetadataTest() throws Exception {

        configurationService.setProperty("bulkedit.allow-bulk-deletion", null);

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-l" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        List<String> infoMessages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(infoMessages, hasSize(1));
        assertThat(infoMessages, hasItem(equalTo("No fields has been configured to be cleared via bulk deletion")));
    }

    @Test
    public void metadataDeletionTest() throws Exception {

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        // Get counts before test - other tests may have created metadata values
        int titleCountBefore = metadataValueService.findByField(context, titleField).size();
        int authorCountBefore = metadataValueService.findByField(context, authorField).size();
        // setup() created 2 items with titles and authors, so we should have at least 2 of each
        assertThat(titleCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));
        assertThat(authorCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));

        // Commit the test data so the script's separate Context can see it
        context.commit();

        configurationService.setProperty("bulkedit.allow-bulk-deletion", "dc.title");

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.title" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        // Use a fresh context for verification since the script ran with its own context
        // and made changes that the test context's Hibernate session doesn't know about
        try (Context verifyContext = new Context(Context.Mode.READ_ONLY)) {
            MetadataField titleFieldFresh = metadataFieldService.findByElement(verifyContext, "dc", "title", null);
            MetadataField authorFieldFresh = metadataFieldService.findByElement(verifyContext, "dc", "contributor",
                    "author");

            // After deletion, all dc.title values should be deleted (including from other tests)
            assertThat(metadataValueService.findByField(verifyContext, titleFieldFresh), empty());
            // Author values should remain unchanged
            assertThat(metadataValueService.findByField(verifyContext, authorFieldFresh), hasSize(authorCountBefore));
        }

        List<String> infoMessages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(infoMessages, hasSize(1));
        assertThat(infoMessages, hasItem(equalTo("Deleting the field 'dc.title' from all objects")));
    }

    @Test
    public void metadataDeletionNotAllowedTest() throws Exception {

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        // Get counts before test - other tests may have created metadata values
        int titleCountBefore = metadataValueService.findByField(context, titleField).size();
        int authorCountBefore = metadataValueService.findByField(context, authorField).size();
        // setup() created 2 items with titles and authors, so we should have at least 2 of each
        assertThat(titleCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));
        assertThat(authorCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));

        configurationService.setProperty("bulkedit.allow-bulk-deletion", "dc.type");

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.title" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        Exception exception = testDSpaceRunnableHandler.getException();
        assertThat(exception, notNullValue());
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("The given metadata field cannot be bulk deleted"));

        // Counts should remain unchanged since deletion was not allowed
        assertEquals(titleCountBefore, metadataValueService.findByField(context, titleField).size());
        assertEquals(authorCountBefore, metadataValueService.findByField(context, authorField).size());
    }

    @Test
    public void metadataDeletionWithUnknownMetadataTest() throws Exception {

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        // Get counts before test - other tests may have created metadata values
        int titleCountBefore = metadataValueService.findByField(context, titleField).size();
        int authorCountBefore = metadataValueService.findByField(context, authorField).size();
        // setup() created 2 items with titles and authors, so we should have at least 2 of each
        assertThat(titleCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));
        assertThat(authorCountBefore, org.hamcrest.Matchers.greaterThanOrEqualTo(2));

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.unknown" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        Exception exception = testDSpaceRunnableHandler.getException();
        assertThat(exception, notNullValue());
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("No metadata field found with name dc.unknown"));

        // Counts should remain unchanged since the metadata field was unknown
        assertEquals(titleCountBefore, metadataValueService.findByField(context, titleField).size());
        assertEquals(authorCountBefore, metadataValueService.findByField(context, authorField).size());
    }

    private void createItem(Collection collection, String title, String author) {
        ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withAuthor(author)
            .build();
    }
}
