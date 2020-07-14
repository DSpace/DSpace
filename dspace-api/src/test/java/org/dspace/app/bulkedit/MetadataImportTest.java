/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTest;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.junit.Rule;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.content.service.WorkspaceItemService;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MetadataImportTest extends AbstractIntegrationTest {

    private final ItemService itemService
        = ContentServiceFactory.getInstance().getItemService();
    private final CollectionService collectionService
        = ContentServiceFactory.getInstance().getCollectionService();
    private final CommunityService communityService
        = ContentServiceFactory.getInstance().getCommunityService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private EntityTypeService entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
    private RelationshipTypeService relationshipTypeService = ContentServiceFactory.getInstance()
                                                                                   .getRelationshipTypeService();
    Collection collection;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void metadataImportTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);
        collection = collectionService.create(context, community);
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
        assertEquals(importedItem.getSubmitter(), eperson);

        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, importedItem.getID()));
        collectionService.delete(context, collectionService.find(context, collection.getID()));
        communityService.delete(context, communityService.find(context, community.getID()));
        context.restoreAuthSystemState();
    }

    @Test(expected = ParseException.class)
    public void metadataImportWithoutEPersonParameterTest()
        throws IllegalAccessException, InstantiationException, ParseException {
        String fileLocation = new File(testProps.get("test.importcsv").toString()).getAbsolutePath();
        String[] args = new String[] {"metadata-import", "-f", fileLocation, "-s"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            script.initialize(args, testDSpaceRunnableHandler, null);
            script.run();
        }
    }

    @Test
    public void relationshipMetadataImportTest() throws Exception {

        String fileLocation = new File(testProps.get("test.importrelationshipcsv").toString()).getAbsolutePath();

        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        item.setSubmitter(context.getCurrentUser());
        itemService.addMetadata(context, item, "relationship", "type", null, null, "Publication");
        itemService.update(context, item);

        workspaceItem = workspaceItemService.create(context, collection, false);
        Item item1 = installItemService.installItem(context, workspaceItem);
        item1.setSubmitter(context.getCurrentUser());
        itemService.addMetadata(context, item1, "relationship", "type", null, null, "Publication");
        itemService.update(context, item1);

        EntityType publication = entityTypeService.create(context, "Publication");
        EntityType person = entityTypeService.create(context, "Person");
        RelationshipType relationshipType = relationshipTypeService.create(context, publication, person,
    "isAuthorOfPublication", "isPublicationOfAuthor", 0, 10, 0, 10);
        context.restoreAuthSystemState();

        List<String> list = Files.readAllLines(Paths.get(fileLocation));
        String lastLine = list.get(list.size() - 1);
        list.remove(list.size() - 1);
        lastLine = lastLine + "\"" + item1.getID() + "\"";
//        lastLine = "\"" + item.getID() + "\"" + lastLine + "\"" + item1.getID() + "\"";
        list.add(lastLine);
        String testFileLocation = testProps.get("test.importrelationshipusedintestcsv").toString();
        Files.write(Paths.get(testFileLocation), list);
        String[] args = new String[] {"metadata-import", "-f", testFileLocation, "-e", eperson.getEmail(), "-v"};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);
        context.turnOffAuthorisationSystem();
        itemService.delete(context, itemService.find(context, item.getID()));
        itemService.delete(context, itemService.find(context, item1.getID()));
        Files.delete(Paths.get(testFileLocation));
        context.commit();
        context.restoreAuthSystemState();
    }
}
