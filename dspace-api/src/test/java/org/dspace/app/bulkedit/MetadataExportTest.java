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
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTest;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

public class MetadataExportTest extends AbstractIntegrationTest {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Test
    public void metadataExportToCsvTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);
        Collection collection = collectionService.create(context, community);
        WorkspaceItem wi = workspaceItemService.create(context, collection, true);
        Item item = wi.getItem();
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Donald, Smith");
        item = installItemService.installItem(context, wi);
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir") + testProps.get("test.exportcsv")
                                                                                        .toString();

        String[] args = new String[] {"metadata-export", "-i", String.valueOf(item.getHandle()), "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, item.getID()));
        collectionService.delete(context, collectionService.find(context, collection.getID()));
        communityService.delete(context, communityService.find(context, community.getID()));
        context.restoreAuthSystemState();
    }
}
