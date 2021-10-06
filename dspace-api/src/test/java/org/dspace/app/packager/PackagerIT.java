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
import org.jdom.Element;
import org.junit.Test;

public class PackagerIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
            .getInstance().getRelationshipMetadataService();
    protected Community child1;
    protected Collection col1;
    protected Item article;
    protected Item author;
    File tempFile;
    File resultFile;

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        try {
            performExportScript(article.getHandle(), tempFile);
            assertTrue(resultFile.length() > 0);
            String idStr = getID();
            assertEquals(idStr, article.getID().toString());
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    @Test
    public void packagerImportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        createTemplate();
        createFiles();
        try {
            performExportScript(article.getHandle(), tempFile);
            String idStr = getID();
            itemService.delete(context, article);
            performImportScript(resultFile);
            Item item = itemService.find(context, UUID.fromString(idStr));
            assertNotNull(item);
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    @Test
    public void packagerExportRelationshipTest() throws Exception {
        //Test sees if the mets created by the packager includes the metadatafields
        //required to do a relationship restore
        context.turnOffAuthorisationSystem();
        createTemplate();
        createRels();
        createFiles();
        try {
            performExportScript(article.getHandle(), tempFile);
            METSManifest manifest = null;
            ZipFile zip = new ZipFile(resultFile);
            ZipEntry manifestEntry = zip.getEntry(METSManifest.MANIFEST_FILE);
            if (manifestEntry != null) {
                // parse the manifest and sanity-check it.
                manifest = METSManifest.create(zip.getInputStream(manifestEntry),
                        false, "AIP");
                for (Element element : manifest.getItemDmds()) {
                    //check to see if the familyName is in the metadata for the article if it is the export
                    // exported the relationship virtual metadata
                    assertTrue(element.getValue().contains("familyName"));
                }
            }
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    @Test
    public void packagerImportRelationshipTest() throws Exception {
        //Tests if a import will restore the relationship of an item
        context.turnOffAuthorisationSystem();
        createTemplate();
        createRels();
        createFiles();
        context.turnOffAuthorisationSystem();
        try {
            performExportScript(article.getHandle(), tempFile);
            List<RelationshipMetadataValue> leftList = relationshipMetadataService
                    .getRelationshipMetadata(article, true);
            assertThat(leftList.size(), equalTo(2));
            assertThat(leftList.get(0).getValue(), equalTo("familyName, firstName"));
            String id = getID();
            performImportScript(resultFile);
            //get the new item create by the import
            Item item2 = itemService.findByIdOrLegacyId(context, id);
            leftList = relationshipMetadataService
                    .getRelationshipMetadata(item2, true);
            //check to see if the metadata exists in the new item
            assertThat(leftList.size(), equalTo(2));
            assertThat(leftList.get(0).getValue(), equalTo("familyName, firstName"));
        } finally {
            tempFile.delete();
            resultFile.delete();
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

        author = ItemBuilder.createItem(context, col1)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .withEntityType("Person")
                .build();
    }

    protected void createRels() {
        //created the entity types needed for creating relations and related them to the items created previously
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType isAuthorOfPublication =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author, isAuthorOfPublication).build();
    }

    protected void createFiles() throws IOException {
        //After packager makes a file it appends a _ITEM@HANDLE.zip to the provided
        //path and thus we need to get the resulting file
        tempFile = File.createTempFile("packagerExportTest", ".zip");
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        resultFile = new File(path + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
    }

    private String getID() throws IOException, MetadataValidationException {
        //this method gets the UUID from the mets file thats stored in the attribute element
        METSManifest manifest = null;
        ZipFile zip = new ZipFile(resultFile);
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
        runDSpaceScript("packager", "-r", "-f", "-z", "*", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
