/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipMetadataService;
import org.dspace.content.RelationshipMetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.jdom.Element;
import org.junit.Test;

// See CsvImportIT for other examples involving rels
public class PackagerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
            .getInstance().getRelationshipMetadataService();

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item article = createTemplate();

        File tempFile = File.createTempFile("packagerExportTest", ".zip");
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        File resultFile = new File(path + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
        try {
            performExportScript(article.getHandle(), tempFile);
            assertTrue(resultFile.length() > 0);
            String idStr = getID(resultFile);
            assertEquals(idStr, article.getID().toString());
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerImportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item article = createTemplate();


        File tempFile = File.createTempFile("packagerExportTest", ".zip");
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        File resultFile = new File(path + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
        try {
            performExportScript(article.getHandle(), tempFile);
            String idStr = getID(resultFile);
            itemService.delete(context, article);
            performImportScript(resultFile);
            Item item = itemService.find(context, UUID.fromString(idStr));
            assertNotNull(item);
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerRelationShipTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        Item article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .withEntityType("Publication")
                .build();
        Item author = ItemBuilder.createItem(context, col1)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .withEntityType("Person")
                .build();
        RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        RelationshipTypeService relationshipTypeService = ContentServiceFactory
                .getInstance().getRelationshipTypeService();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType isAuthorOfPublication =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author, isAuthorOfPublication).build();
        File tempFile = File.createTempFile("packagerExportTest", ".zip");
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        File resultFile = new File(path + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
        try {
            List<RelationshipMetadataValue> leftList = relationshipMetadataService
                    .getRelationshipMetadata(article, true);
            assertThat(leftList.size(), equalTo(2));
            assertThat(leftList.get(0).getValue(), equalTo("familyName, firstName"));
            performExportScript(article.getHandle(), tempFile);
            getID(resultFile);
            performImportScript(resultFile);
            Item item = itemService.find(context, article.getID());
            leftList = relationshipMetadataService
                    .getRelationshipMetadata(item, true);
            assertThat(leftList.size(), equalTo(2));
            assertThat(leftList.get(0).getValue(), equalTo("familyName, firstName"));
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            tempFile.delete();
        }
    }

    private Item createTemplate() {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        // Create a new Publication (which is an Article)
        return ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .withEntityType("Publication")
                .build();
    }

    private String getID(File tempFile) throws IOException, MetadataValidationException {
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
