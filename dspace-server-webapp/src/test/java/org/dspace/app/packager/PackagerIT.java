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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.ItemService;
import org.jdom.Element;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

// See CsvImportIT for other examples involving rels
public class PackagerIT extends AbstractEntityIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    @Order(1)
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community child1 = null;
        Collection col1 = null;
        Item article = null;
        article = createTemplate(child1, col1, article);

        File tempFile = File.createTempFile("packagerExportTest", ".zip");
        try {
            performExportScript(article.getHandle(), tempFile);
            assertTrue(tempFile.length() > 0);
            String idStr = getID(tempFile);
            assertEquals(idStr, article.getID().toString());
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    @Order(2)
    public void packagerImportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community child1 = null;
        Collection col1 = null;
        Item article = null;
        article = createTemplate(child1, col1, article);


        File tempFile = File.createTempFile("packagerExportTest", ".zip");
        try {
            performExportScript(article.getHandle(), tempFile);
            String idStr = getID(tempFile);
            itemService.delete(context, article);
            performImportScript(tempFile);
            System.out.println(idStr);
            Item item = itemService.find(context, UUID.fromString(idStr));
            assertNotNull(item);
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            tempFile.delete();
        }
    }

    private Item createTemplate(Community child1, Collection col1, Item article) {
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
        return article;
    }

    private String getID(File tempFile) throws IOException, MetadataValidationException {
        METSManifest manifest = null;
        System.out.println(tempFile.getAbsolutePath());
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
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle, "-t", "AIP", outputFile.getPath());
    }

    private void performImportScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-u", "-e", "admin@email.com", "-t", "AIP", outputFile.getPath());
    }
}
