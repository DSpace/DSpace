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
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTest;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.junit.Before;
import org.junit.Test;

public class MetadataImportTest extends AbstractIntegrationTest {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();

    @Before
    public void setup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);
        Collection collection = collectionService.create(context, community);
        context.restoreAuthSystemState();
    }
    @Test
    public void metadataImportTest() throws Exception {
        String fileLocation = new File(testProps.get("test.importcsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        Item importedItem = itemService.findAll(context).next();
        List<MetadataValue> metadata = itemService.getMetadata(importedItem, "dc", "contributor", "author", Item.ANY);
        assertTrue(StringUtils.equals(metadata.get(0).getValue(), "Donald, SmithImported"));
        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.commit();
        context.restoreAuthSystemState();

    }

    @Test
    public void personMetadataImportTest() throws Exception {

        String fileLocation = new File(testProps.get("test.importpersoncsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-e", eperson.getEmail(), "-s"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        Item importedItem = itemService.findAll(context).next();
        assertTrue(
            StringUtils.equals(
                itemService.getMetadata(importedItem, "person", "birthDate", null, Item.ANY)
                           .get(0).getValue(), "2000"));
        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        context.commit();
        context.restoreAuthSystemState();
    }
}
