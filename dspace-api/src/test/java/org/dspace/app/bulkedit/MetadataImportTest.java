/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static junit.framework.TestCase.assertTrue;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTest;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

public class MetadataImportTest extends AbstractIntegrationTest {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Test
    public void metadataImportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);
        Collection collection = collectionService.create(context, community);
        context.restoreAuthSystemState();

        String fileLocation = new File(testProps.get("test.importcsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        Item importedItem = itemService.findAll(context).next();
        assertTrue(
            StringUtils.equals(
                itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY).get(0).getValue(),
                "Donald, SmithImported"));

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        collectionService.delete(context, collectionService.find(context, collection.getID()));
        communityService.delete(context, communityService.find(context, community.getID()));
        context.restoreAuthSystemState();
    }
}
