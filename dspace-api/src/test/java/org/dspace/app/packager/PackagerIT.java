/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Iterators;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic integration testing for the Packager restore feature
 *
 * @author Nathan Buckingham
 */
public class PackagerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    protected static final InstallItemService installItemService = ContentServiceFactory.getInstance()
            .getInstallItemService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected Community child1;
    protected Collection col1;
    protected Item article;
    File tempFile;

    @Before
    public void setup() throws IOException {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                                .withEntityType("Publication").build();

        // Create a new Publication (which is an Article)
        article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .build();

        tempFile = File.createTempFile("packagerExportTest", ".zip");
        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        tempFile.delete();
        super.destroy();
    }

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        performExportScript(article.getHandle(), tempFile);
        assertTrue(tempFile.length() > 0);
        String idStr = getID();
        assertEquals(idStr, article.getID().toString());
    }

    @Test
    public void packagerImportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //Item
        performExportScript(article.getHandle(), tempFile);
        String idStr = getID();
        itemService.delete(context, article);
        performImportScript(tempFile);
        Item item = itemService.find(context, UUID.fromString(idStr));
        assertNotNull(item);
    }

    @Test
    public void packagerImportColUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configService.setProperty("upload.temp.dir",tempFile.getParent());

        performExportScript(col1.getHandle(), tempFile);
        String idStr = getID();
        collectionService.delete(context, col1);
        performImportScript(tempFile);
        Collection collection = collectionService.find(context, UUID.fromString(idStr));
        assertNotNull(collection);
    }

    @Test
    public void packagerImportComUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configService.setProperty("upload.temp.dir",tempFile.getParent());

        //Community
        performExportScript(child1.getHandle(), tempFile);
        String idStr = getID();
        communityService.delete(context, child1);
        performImportScript(tempFile);
        Community community = communityService.find(context, UUID.fromString(idStr));
        assertNotNull(community);
    }

    @Test
    public void packagerUUIDAlreadyExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //Item should be overwritten if UUID already Exists
        performExportScript(article.getHandle(), tempFile);
        performImportScript(tempFile);
        Iterator<Item> items = itemService.findByCollection(context, col1);
        assertEquals(1, Iterators.size(items));
    }

    @Test
    public void packagerUUIDAlreadyExistWithoutForceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //should fail to restore the item because the uuid already exists.
        performExportScript(article.getHandle(), tempFile);
        UUID id = article.getID();
        itemService.delete(context, article);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, id, false);
        installItemService.installItem(context, workspaceItem, "123456789/0100");
        performImportNoForceScript(tempFile);
        Iterator<Item> items = itemService.findByCollection(context, col1);
        Item testItem = items.next();
        assertFalse(items.hasNext()); //check to make sure there is only 1 item
        assertEquals("123456789/0100", testItem.getHandle()); //check to make sure the item wasn't overwritten as
        // it would have the old handle.
        itemService.delete(context, testItem);
    }

    private String getID() throws IOException, MetadataValidationException {
        //this method gets the UUID from the mets file thats stored in the attribute element
        METSManifest manifest = null;
        ZipFile zip = new ZipFile(tempFile);
        ZipEntry manifestEntry = zip.getEntry(METSManifest.MANIFEST_FILE);
        if (manifestEntry != null) {
            // parse the manifest and sanity-check it.
            manifest = METSManifest.create(zip.getInputStream(manifestEntry),
                    false, "AIP");
        }
        Element mets = manifest.getMets();
        String idStr = mets.getAttributeValue("ID");
        if (idStr.contains("DB-ID-")) {
            idStr = idStr.substring(idStr.lastIndexOf("DB-ID-") + 6, idStr.length());
        }
        return idStr;
    }


    private void performExportScript(String handle, File outputFile) throws Exception {
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle, "-t",
                "AIP", outputFile.getPath());
    }

    private void performImportNoForceScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }

    private void performImportScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
