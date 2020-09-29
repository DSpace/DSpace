/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.ListDSpaceRunnableHandler;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.junit.Test;

public class BulkImportIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    @Test
    public void testImport() throws Exception {

        context.turnOffAuthorisationSystem();

        Community community = createCommunity(context).build();
        Collection collection = createCollection(context, community).withAdminGroup(eperson).build();
        Item itemToUpdateByOrcid = createItem(context, collection).withOrcidIdentifier("0000-0002-7532-0899").build();
        Item itemToDelete = createItem(context, collection).withDoiIdentifier("10.1000/182").build();

        context.restoreAuthSystemState();

        String fileLocation = getXlsFile("test-ok.xls").getAbsolutePath();
        String[] args = new String[] { "bulk-import", "-c", collection.getID().toString(), "-f", fileLocation };
        ListDSpaceRunnableHandler listDSpaceRunnableHandler = new ListDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), listDSpaceRunnableHandler, kernelImpl, eperson);
        assertTrue("Expected no errors", listDSpaceRunnableHandler.getErrorMessages().isEmpty());
        assertTrue("Expected no warnings", listDSpaceRunnableHandler.getWarningMessages().isEmpty());

        List<String> infoMessages = listDSpaceRunnableHandler.getInfoMessages();
        assertEquals("Expected 4 info messages", 4, infoMessages.size());

        assertEquals("Row 2 - Item created successfully", infoMessages.get(0));
        assertEquals("Row 3 - Item updated successfully", infoMessages.get(0));
        assertEquals("Row 4 - Item updated successfully", infoMessages.get(0));
        assertEquals("Row 5 - Item created successfully", infoMessages.get(0));

        assertNull("Item expected to be deleted", itemService.find(context, itemToDelete.getID()));
    }

    private File getXlsFile(String name) {
        return new File(BASE_XLS_DIR_PATH, name);
    }
}
