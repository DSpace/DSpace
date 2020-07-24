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
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

public class MetadataExportIT
        extends AbstractIntegrationTestWithDatabase {
    private final ItemService itemService
            = ContentServiceFactory.getInstance().getItemService();
    private final InstallItemService installItemService
            = ContentServiceFactory.getInstance().getInstallItemService();
    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Test
    public void metadataExportToCsvTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        WorkspaceItem wi = WorkspaceItemBuilder.createWorkspaceItem(context, collection).build();
        Item item = wi.getItem();
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Donald, Smith");
        item = installItemService.installItem(context, wi);
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir")
                + testProps.get("test.exportcsv").toString();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(item.getHandle()),
            "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
                = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
                testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, item.getID()));
        context.restoreAuthSystemState();
    }
}
