/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void metadataDeletionTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        createItem(collection, "My First publication", "Mario Rossi");
        createItem(collection, "Another publication", "John Smith");
        context.restoreAuthSystemState();

        MetadataField titleField = metadataFieldService.findByElement(context, "dc", "title", null);
        MetadataField authorField = metadataFieldService.findByElement(context, "dc", "contributor", "author");

        assertEquals(2, metadataValueService.findByField(context, titleField).size());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        String[] args = new String[] { "metadata-deletion", "-m", "dc.title" };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertTrue(metadataValueService.findByField(context, titleField).isEmpty());
        assertEquals(2, metadataValueService.findByField(context, authorField).size());
    }

    private void createItem(Collection collection, String title, String author) {
        ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withAuthor(author)
            .build();
    }
}
