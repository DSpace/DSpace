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
import static org.junit.Assert.assertEquals;

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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

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

    @Before
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

        assertThat(metadataValueService.findByField(context, titleField), hasSize(2));
        assertThat(metadataValueService.findByField(context, authorField), hasSize(2));

        configurationService.setProperty("bulkedit.allow-bulk-deletion", "dc.title");

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.title" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(metadataValueService.findByField(context, titleField), empty());
        assertThat(metadataValueService.findByField(context, authorField), hasSize(2));

        List<String> infoMessages = testDSpaceRunnableHandler.getInfoMessages();
        assertThat(infoMessages, hasSize(1));
        assertThat(infoMessages, hasItem(equalTo("Deleting the field 'dc.title' from all objects")));
    }

    @Test
    public void metadataDeletionNotAllowedTest() throws Exception {

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        assertEquals(2, metadataValueService.findByField(context, titleField).size());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());

        configurationService.setProperty("bulkedit.allow-bulk-deletion", "dc.type");

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.title" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        Exception exception = testDSpaceRunnableHandler.getException();
        assertThat(exception, notNullValue());
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("The given metadata field cannot be bulk deleted"));

        assertEquals(2, metadataValueService.findByField(context, titleField).size());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());
    }

    @Test
    public void metadataDeletionWithUnknownMetadataTest() throws Exception {

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        assertEquals(2, metadataValueService.findByField(context, titleField).size());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.unknown" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        Exception exception = testDSpaceRunnableHandler.getException();
        assertThat(exception, notNullValue());
        assertThat(exception, instanceOf(IllegalArgumentException.class));
        assertThat(exception.getMessage(), is("No metadata field found with name dc.unknown"));

        assertEquals(2, metadataValueService.findByField(context, titleField).size());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());
    }

    private void createItem(Collection collection, String title, String author) {
        ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withAuthor(author)
            .build();
    }
}
