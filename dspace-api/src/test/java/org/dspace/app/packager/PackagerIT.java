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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
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
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipMetadataService;
import org.dspace.content.RelationshipMetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.CollectionService;
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
    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
            .getInstance().getRelationshipMetadataService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private final InstallItemService installItemService = ContentServiceFactory.getInstance()
            .getInstallItemService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    Community child1;
    Collection col1;
    Collection col2;
    Item article;
    Item author;
    Relationship relationship;
    File tempFile;
    File resultFile;

    @Before
    public void setup() throws IOException {
        context.turnOffAuthorisationSystem();
        //created the entity types needed for creating relations and related them to the items created previously
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType isAuthorOfPublication =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        0, 10, 0, 10).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 2")
                .withEntityType("Publication")
                .build();

        col2 = CollectionBuilder.createCollection(context, child1)
                .withName("Person Collection")
                .withEntityType("Person")
                .build();

        // Create a new Publication (which is an Article)
        article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .build();

        author = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .build();

        relationship = RelationshipBuilder.createRelationshipBuilder(context,
                article, author, isAuthorOfPublication).build();

        // After packager makes a file it appends a _ITEM@HANDLE.zip to the provided
        // path, and thus we need to get the resulting file
        tempFile = File.createTempFile("packagerExportTest", ".zip");
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        resultFile = new File(path + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        tempFile.delete();
        resultFile.delete();
        // Packager sometimes creates related files to community / collection we need to clean those related files up
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, "packagerExportTest*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
        super.destroy();
    }

    @Test
    public void packagerExportUUIDTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();
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
        try {
            performExportScript(article.getHandle(), tempFile);
            String idStr = getID();
            context.turnOffAuthorisationSystem();
            // Running the script puts the context in a weird state which causes rels to be null
            // So calling commit to fix this
            context.commit();
            itemService.delete(context, itemService.find(context, article.getID()));
            context.restoreAuthSystemState();
            performImportScript(resultFile);
            Item item = itemService.find(context, UUID.fromString(idStr));
            assertNotNull(item);
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    @Test
    public void packagerImportColUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configService.setProperty("upload.temp.dir", tempFile.getParent());

        performExportScript(col1.getHandle(), tempFile);
        context.commit();
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        resultFile = new File(path + "_COLLECTION@" + col1.getHandle().replace("/", "-") + ".zip");
        String idStr = getID();
        collectionService.delete(context, collectionService.find(context, col1.getID()));
        performImportScript(resultFile);
        Collection collection = collectionService.find(context, UUID.fromString(idStr));
        assertNotNull(collection);
    }

    @Test
    public void packagerUUIDAlreadyExistWithoutForceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //should fail to restore the item because the uuid already exists.
        performExportScript(article.getHandle(), tempFile);
        UUID id = article.getID();
        String handle = article.getHandle();
        context.commit();
        itemService.delete(context, itemService.find(context, id));
        WorkspaceItem workspaceItem = workspaceItemService.create(context, col1, id, false);
        installItemService.installItem(context, workspaceItem, "123456789/1000");
        performImportNoForceScript(resultFile);
        Iterator<Item> items = itemService.findByCollection(context, col1);
        Item testItem = items.next();
        assertFalse(items.hasNext()); // check to make sure there is only 1 item
        assertNotEquals("123456789/1000", handle); //check to make sure the item wasn't overwritten as
        assertEquals(testItem.getID(), id);
        itemService.delete(context, testItem);
    }

    @Test
    public void packagerExportRelationshipTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();
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
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();
            List<RelationshipMetadataValue> leftList = relationshipMetadataService
                    .getRelationshipMetadata(itemService.find(context, article.getID()), true);
            assertThat(leftList.size(), equalTo(3));
            assertThat(leftList, hasItem(hasProperty("value", equalTo("familyName, firstName"))));
            String id = getID();
            performImportScript(resultFile);
            //get the new item create by the import
            Item item2 = itemService.findByIdOrLegacyId(context, id);
            leftList = relationshipMetadataService
                    .getRelationshipMetadata(item2, true);
            //check to see if the metadata exists in the new item
            assertThat(leftList.size(), equalTo(3));
            assertThat(leftList, hasItem(hasProperty("value", equalTo("familyName, firstName"))));
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
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

    private void performImportNoForceScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }

    private void performImportScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-z", "*", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
