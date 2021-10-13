/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.AbstractIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Test;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class MetadataExportIT
        extends AbstractIntegrationTest {
    private final ContentServiceFactory contentServiceFactory;
    private final CommunityService communityService;
    private final CollectionService collectionService;
    private final WorkspaceItemService workspaceItemService;
    private final ItemService itemService;

    public MetadataExportIT() {
        contentServiceFactory = ContentServiceFactory.getInstance();
        communityService = contentServiceFactory.getCommunityService();
        collectionService = contentServiceFactory.getCollectionService();
        workspaceItemService = contentServiceFactory.getWorkspaceItemService();
        itemService = contentServiceFactory.getItemService();
    }

    /**
     * Test of export with mapped Item.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testMappedItem()
            throws Exception {
        System.out.println("export");

        // Create some content with which to test.
        context.turnOffAuthorisationSystem();

        Community parentCommunity = communityService.create(null, context);

        Collection collection1 = collectionService.create(context, parentCommunity);
        Collection collection2 = collectionService.create(context, parentCommunity);

        context.setCurrentUser(eperson);
        WorkspaceItem workspaceItem = workspaceItemService
                .create(context, collection1, false);
        Item item = contentServiceFactory.getInstallItemService()
                .installItem(context, workspaceItem);
        collectionService.addItem(context, collection2, item);

        context.restoreAuthSystemState();

        // Test!
        MetadataExport instance = new MetadataExport(context, parentCommunity, false);
        DSpaceCSV result = instance.export();
        context.commit();

        // Clean up.
        context.turnOffAuthorisationSystem();
        try { // Just swallow any errors during cleanup
            item = itemService.find(context, item.getID());
            itemService.delete(context, item);
            collectionService.delete(context, collection2);
            collectionService.delete(context, collection1);
            communityService.delete(context, parentCommunity);
        } catch (Exception e) {
            // Do nothing
        }
        context.restoreAuthSystemState();

        // Examine the result.
        List<DSpaceCSVLine> csvLines = result.getCSVLines();
        assertEquals("One item mapped twice should produce one line",
                1, csvLines.size());
    }
}
