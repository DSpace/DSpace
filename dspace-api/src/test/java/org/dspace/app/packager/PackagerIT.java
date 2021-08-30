/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import static org.junit.Assert.assertEquals;
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
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Element;
import org.junit.Test;

public class PackagerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected Community child1;
    protected Collection col1;
    protected Item article;
    File tempFile;

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        try {
            performExportScript(article.getHandle(), tempFile);
            assertTrue(tempFile.length() > 0);
            String idStr = getID();
            assertEquals(idStr, article.getID().toString());
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerImportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        try {
            //Item
            performExportScript(article.getHandle(), tempFile);
            String idStr = getID();
            itemService.delete(context, article);
            performImportScript(tempFile);
            Item item = itemService.find(context, UUID.fromString(idStr));
            assertNotNull(item);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerImportColUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        configService.setProperty("upload.temp.dir",tempFile.getParent());
        try {
            performExportScript(col1.getHandle(), tempFile);
            String idStr = getID();
            collectionService.delete(context, col1);
            performImportScript(tempFile);
            Collection collection = collectionService.find(context, UUID.fromString(idStr));
            assertNotNull(collection);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerImportComUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        configService.setProperty("upload.temp.dir",tempFile.getParent());
        try {
            //Community
            performExportScript(child1.getHandle(), tempFile);
            String idStr = getID();
            communityService.delete(context, child1);
            performImportScript(tempFile);
            Community community = communityService.find(context, UUID.fromString(idStr));
            assertNotNull(community);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerUUIDAlreadyExistTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        try {
            //Item should be overwritten if UUID already Exists
            performExportScript(article.getHandle(), tempFile);
            performImportScript(tempFile);
            Iterator<Item> items = itemService.findByCollection(context, col1);
            assertEquals(1, Iterators.size(items));
        } finally {
            tempFile.delete();
        }
    }

    protected void createTemplate() {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        // Create a new Publication (which is an Article)
        article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .withEntityType("Publication")
                .build();
    }

    protected void createFiles() throws IOException {
        tempFile = File.createTempFile("packagerExportTest", ".zip");
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

    private void performImportScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
