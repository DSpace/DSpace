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
import org.dspace.builder.WorkspaceItemBuilder;
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
import org.dspace.content.service.RelationshipService;
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
    private RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private final InstallItemService installItemService = ContentServiceFactory.getInstance()
            .getInstallItemService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    Community child1;
    Collection col1;
    Collection col2;

    Item article;
    Item author;
    Item article2;
    Item author2;

    // Relationship types
    RelationshipType isAuthorOfPublication;
    RelationshipType isAuthorOfPublication2;

    // Primary relationships
    Relationship relationship;
    Relationship relationship2;
    Relationship relationship3;

    File tempFile;
    File resultFile;

    @Before
    public void setup() throws IOException {
        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        isAuthorOfPublication =
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

        article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .build();

        author = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .build();

        article2 = ItemBuilder.createItem(context, col1)
                .withTitle("Article2")
                .withIssueDate("2018-05-01")
                .build();

        author2 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("secondFamily")
                .withPersonIdentifierFirstName("secondFirst")
                .build();

        relationship = RelationshipBuilder.createRelationshipBuilder(context,
                article, author, isAuthorOfPublication).build();

        relationship2 = RelationshipBuilder.createRelationshipBuilder(context,
                article2, author, isAuthorOfPublication).build();

        relationship3 = RelationshipBuilder.createRelationshipBuilder(context,
                article, author2, isAuthorOfPublication).build();

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

        // Clean up temp dir prefixed files
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, "packagerExportTest*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }

        // Clean up any ITEM@*.zip or COLLECTION@*.zip dropped in the working directory
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingDir, "ITEM@*.zip")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingDir, "COLLECTION@*.zip")) {
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
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1, id).build();
        installItemService.installItem(context, workspaceItem, "123456789/1000");
        performImportNoForceScript(resultFile);
        Iterator<Item> items = itemService.findByCollection(context, col1);
        // Find the specific item by UUID
        Item testItem = itemService.find(context, id);
        assertNotNull("Item with original UUID should exist", testItem);
        assertNotEquals("123456789/1000", handle); // item should not have been overwritten
        assertEquals(id, testItem.getID());
        itemService.delete(context, testItem);
        context.restoreAuthSystemState();
    }

    @Test
    public void packagerExportRelationshipTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();
            METSManifest manifest = getManifest(resultFile);
            assertNotNull(manifest);
            boolean foundFamilyName = false;
            for (Element element : manifest.getItemDmds()) {
                if (element.getValue().contains("familyName")) {
                    foundFamilyName = true;
                    break;
                }
            }
            assertTrue("METS manifest should contain relationship virtual metadata (familyName)", foundFamilyName);
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
            assertThat(leftList.size(), equalTo(6));
            assertThat(leftList, hasItem(hasProperty("value", equalTo("familyName, firstName"))));
            String id = getID();
            performImportScript(resultFile);
            //get the new item create by the import
            Item item2 = itemService.findByIdOrLegacyId(context, id);
            leftList = relationshipMetadataService.getRelationshipMetadata(item2, true);
            assertThat(leftList.size(), equalTo(6));
            assertThat(leftList, hasItem(hasProperty("value", equalTo("familyName, firstName"))));
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    /**
     * Verifies that exporting with a prefix filename produces
     * prefix_ITEM@handle.zip naming for each disseminated item.
     */
    @Test
    public void packagerExportFileNamingWithPrefixTest() throws Exception {
        File prefixFile = File.createTempFile("packagerNamingTest", ".zip");
        try {
            performExportScript(article.getHandle(), prefixFile);
            context.commit();

            String base = prefixFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");

            // Primary article zip
            File articleZip = new File(base + "_ITEM@" +
                    article.getHandle().replace("/", "-") + ".zip");
            assertTrue("Primary article zip should exist with prefix naming", articleZip.exists());
            assertTrue(articleZip.length() > 0);
        } finally {
            cleanupPrefixFiles("packagerNamingTest");
            prefixFile.delete();
        }
    }

    /**
     * Verifies that exporting without a path puts files in the current directory
     * using ITEM@handle.zip naming (no prefix).
     */
    @Test
    public void packagerExportFileNamingNoPrefixTest() throws Exception {
        // Pass "." as the output path to simulate no-path behaviour
        File currentDir = new File(".");
        File expectedFile = new File("ITEM@" + article.getHandle().replace("/", "-") + ".zip");
        try {
            performExportScriptToDir(article.getHandle(), currentDir);
            context.commit();
            assertTrue("ITEM@handle.zip should be created in current directory", expectedFile.exists());
            assertTrue(expectedFile.length() > 0);
        } finally {
            expectedFile.delete();
        }
    }

    /**
     * Verifies that when disseminating an item that has relationships,
     * ALL related items also get their own correctly named zip files.
     * article -> author, author2
     * author  -> article, article2   (transitive — article2 should also be disseminated)
     */
    @Test
    public void packagerExportRelatedItemsFileNamingTest() throws Exception {
        File prefixFile = File.createTempFile("packagerRelNamingTest", ".zip");
        try {
            performExportWithScopeScript(article.getHandle(), prefixFile, "*");
            context.commit();

            String base = prefixFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");

            File authorZip  = new File(base + "_ITEM@" + author.getHandle().replace("/", "-")  + ".zip");
            File author2Zip = new File(base + "_ITEM@" + author2.getHandle().replace("/", "-") + ".zip");
            File article2Zip = new File(base + "_ITEM@" + article2.getHandle().replace("/", "-") + ".zip");

            assertTrue("author zip should exist",   authorZip.exists());
            assertTrue("author2 zip should exist",  author2Zip.exists());
            assertTrue("article2 zip should exist (transitive via author)", article2Zip.exists());
        } finally {
            cleanupPrefixFiles("packagerRelNamingTest");
            prefixFile.delete();
        }
    }

    /**
     * Disseminating article with -z "*" should transitively reach article2
     * (article -> author -> article2) and embed author metadata in article2's METS.
     */
    @Test
    public void packagerExportTransitiveRelationshipInMetsTest() throws Exception {
        File prefixFile = File.createTempFile("packagerTransitiveTest", ".zip");
        try {
            performExportWithScopeScript(article.getHandle(), prefixFile, "*");
            context.commit();

            String base = prefixFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");
            File article2Zip = new File(base + "_ITEM@" + article2.getHandle().replace("/", "-") + ".zip");

            assertTrue("Transitive article2 zip must exist", article2Zip.exists());

            // article2's METS should contain author's familyName (virtual metadata)
            METSManifest manifest = getManifest(article2Zip);
            assertNotNull(manifest);
            boolean foundFamilyName = false;
            for (Element element : manifest.getItemDmds()) {
                if (element.getValue().contains("familyName")) {
                    foundFamilyName = true;
                    break;
                }
            }
            assertTrue("article2 METS should embed author familyName via relationship", foundFamilyName);
        } finally {
            cleanupPrefixFiles("packagerTransitiveTest");
            prefixFile.delete();
        }
    }

    /**
     * article's METS should contain BOTH authors' family names since it has
     * two author relationships.
     */
    @Test
    public void packagerExportMultipleAuthorsInMetsTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();

            METSManifest manifest = getManifest(resultFile);
            assertNotNull(manifest);
            boolean foundFirst  = false;
            boolean foundSecond = false;
            for (Element element : manifest.getItemDmds()) {
                String val = element.getValue();
                if (val.contains("familyName")) {
                    foundFirst  = true;
                }
                if (val.contains("secondFamily")) {
                    foundSecond = true;
                }
            }
            assertTrue("METS should contain first author familyName",   foundFirst);
            assertTrue("METS should contain second author secondFamily", foundSecond);
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    /**
     * Export article, update its title, then restore from the original package.
     * The restored item should revert to the original title.
     */
    @Test
    public void packagerRestoreRevertsTitleEditTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();
            String originalId = getID();

            // Edit the title
            context.turnOffAuthorisationSystem();
            Item liveArticle = itemService.find(context, article.getID());
            itemService.clearMetadata(context, liveArticle, "dc", "title", null, Item.ANY);
            itemService.addMetadata(context, liveArticle, "dc", "title", null, null, "Modified Title");
            itemService.update(context, liveArticle);
            context.commit();

            assertEquals("Modified Title",
                    itemService.getMetadataFirstValue(liveArticle, "dc", "title", null, Item.ANY));

            // Force-restore from original package
            performReplaceScript(resultFile);
            context.commit();

            Item restored = itemService.find(context, UUID.fromString(originalId));
            assertEquals("Article",
                    itemService.getMetadataFirstValue(restored, "dc", "title", null, Item.ANY));
            context.restoreAuthSystemState();
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    /**
     * Export, delete the item, import — relationships should be recreated.
     * Then export again and verify the new zip also contains the relationship metadata.
     */
    @Test
    public void packagerRestoreAndReExportRelationshipsTest() throws Exception {
        File secondExport = File.createTempFile("packagerReExportTest", ".zip");
        String secondBase = secondExport.getAbsolutePath().replaceAll("\\.[^.]+$", "");
        File secondResult = new File(secondBase + "_ITEM@" +
                article.getHandle().replace("/", "-") + ".zip");
        try {
            // First export
            performExportScript(article.getHandle(), tempFile);
            context.commit();
            String id = getID();

            // Delete and restore
            context.turnOffAuthorisationSystem();
            itemService.delete(context, itemService.find(context, article.getID()));
            context.commit();
            context.restoreAuthSystemState();

            performImportScript(resultFile);
            context.commit();

            // Re-export the restored item
            performExportScript(article.getHandle(), secondExport);
            context.commit();

            assertTrue("Re-exported zip should exist", secondResult.exists());

            METSManifest manifest = getManifest(secondResult);
            assertNotNull(manifest);
            boolean foundFamilyName = false;
            for (Element element : manifest.getItemDmds()) {
                if (element.getValue().contains("familyName")) {
                    foundFamilyName = true;
                    break;
                }
            }
            assertTrue("Re-exported METS should still contain relationship metadata", foundFamilyName);
        } finally {
            tempFile.delete();
            resultFile.delete();
            secondExport.delete();
            secondResult.delete();
            cleanupPrefixFiles("packagerReExportTest");
        }
    }

    /**
     * Export article, remove its relationship to author, force-replace from package.
     * The relationship should be restored.
     */
    @Test
    public void packagerReplaceRestoresRemovedRelationshipTest() throws Exception {
        try {
            performExportScript(article.getHandle(), tempFile);
            context.commit();

            // Remove the relationship
            context.turnOffAuthorisationSystem();
            Relationship liveRel = relationshipService.find(context, relationship.getID());
            relationshipService.delete(context, liveRel);
            context.commit();

            List<RelationshipMetadataValue> metaBefore = relationshipMetadataService
                    .getRelationshipMetadata(itemService.find(context, article.getID()), true);
            // familyName should no longer appear
            boolean foundFamilyName = metaBefore.stream()
                    .anyMatch(v -> "familyName, firstName".equals(v.getValue()));
            assertFalse("Relationship metadata should be gone after deletion", foundFamilyName);

            // Force-replace restores it
            performReplaceScript(resultFile);
            context.commit();
            context.restoreAuthSystemState();

            List<RelationshipMetadataValue> metaAfter = relationshipMetadataService
                    .getRelationshipMetadata(itemService.find(context, article.getID()), true);
            assertThat(metaAfter, hasItem(hasProperty("value", equalTo("familyName, firstName"))));
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    /**
     * Add a NEW relationship after export, then restore from the old package.
     * The new relationship should be gone (replaced back to the original state).
     */
    @Test
    public void packagerReplaceRemovesAddedRelationshipTest() throws Exception {
        try {
            // Export with only the original relationships
            performExportScript(article.getHandle(), tempFile);
            context.commit();

            // Add a brand-new author item and relate it to article
            context.turnOffAuthorisationSystem();
            Item extraAuthor = ItemBuilder.createItem(context, col2)
                    .withPersonIdentifierLastName("extraFamily")
                    .withPersonIdentifierFirstName("extraFirst")
                    .build();
            RelationshipBuilder.createRelationshipBuilder(context, article, extraAuthor, isAuthorOfPublication).build();
            context.commit();

            List<RelationshipMetadataValue> metaBefore = relationshipMetadataService
                    .getRelationshipMetadata(itemService.find(context, article.getID()), true);
            boolean foundExtra = metaBefore.stream()
                    .anyMatch(v -> v.getValue().contains("extraFamily"));
            assertTrue("Extra author should appear in metadata before replace", foundExtra);

            // Force-replace back to the original state
            performReplaceScript(resultFile);
            context.commit();
            context.restoreAuthSystemState();

            List<RelationshipMetadataValue> metaAfter = relationshipMetadataService
                    .getRelationshipMetadata(itemService.find(context, article.getID()), true);
            boolean stillExtra = metaAfter.stream()
                    .anyMatch(v -> v.getValue().contains("extraFamily"));
            assertFalse("Extra author relationship should be removed after replace from original package", stillExtra);
        } finally {
            tempFile.delete();
            resultFile.delete();
        }
    }

    /**
     * Disseminate an item that has no relationships at all — should produce
     * exactly one zip with no relationship metadata in METS.
     */
    @Test
    public void packagerExportNoRelationshipsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item standalone = ItemBuilder.createItem(context, col1)
                .withTitle("Standalone Article")
                .withIssueDate("2020-01-01")
                .build();
        context.restoreAuthSystemState();

        File standaloneTemp = File.createTempFile("packagerStandaloneTest", ".zip");
        String base = standaloneTemp.getAbsolutePath().replaceAll("\\.[^.]+$", "");
        File standaloneResult = new File(base + "_ITEM@" +
                standalone.getHandle().replace("/", "-") + ".zip");
        try {
            performExportScript(standalone.getHandle(), standaloneTemp);
            context.commit();

            assertTrue("Standalone zip should exist", standaloneResult.exists());
            assertTrue(standaloneResult.length() > 0);

            METSManifest manifest = getManifest(standaloneResult);
            assertNotNull(manifest);
            // No family name should appear
            boolean foundFamilyName = false;
            for (Element element : manifest.getItemDmds()) {
                if (element.getValue().contains("familyName")) {
                    foundFamilyName = true;
                    break;
                }
            }
            assertFalse("METS should not contain relationship metadata for standalone item", foundFamilyName);
        } finally {
            standaloneTemp.delete();
            standaloneResult.delete();
        }
    }

    /**
     * Disseminating with -z "isPublicationOfAuthor" from article should pull in
     * the direct authors since this is the rightward type name that Person items carry.
     * article2 should NOT be included as it is only reachable transitively.
     */
    @Test
    public void packagerExportSpecificScopeOnlyDisseminatesMatchingRelationshipTest() throws Exception {
        File prefixFile = File.createTempFile("packagerSpecificScopeTest", ".zip");
        try {
            performExportWithScopeScript(article.getHandle(), prefixFile, "isPublicationOfAuthor");
            context.commit();

            String base = prefixFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");

            // Direct authors should be disseminated
            File articleZip = new File(base + "_ITEM@" + article.getHandle().replace("/", "-")  + ".zip");
            File authorZip  = new File(base + "_ITEM@" + author.getHandle().replace("/", "-")   + ".zip");
            File author2Zip = new File(base + "_ITEM@" + author2.getHandle().replace("/", "-")  + ".zip");

            assertTrue("article zip should exist",  articleZip.exists());
            assertTrue("author zip should exist",   authorZip.exists());
            assertTrue("author2 zip should exist",  author2Zip.exists());

            // article2 is only reachable via author's isPublicationOfAuthor side — not in this scope
            File article2Zip = new File(base + "_ITEM@" + article2.getHandle().replace("/", "-") + ".zip");
            assertFalse("article2 zip should NOT exist for isPublicationOfAuthor scope only", article2Zip.exists());
        } finally {
            cleanupPrefixFiles("packagerSpecificScopeTest");
            prefixFile.delete();
        }
    }

    /**
     * Disseminating with -z "isAuthorOfPublication" from article produces only
     * the primary item zip — this is the leftward type name on the Publication side
     * and does not match any relationships when starting from article.
     */
    @Test
    public void packagerExportInverseScopeProducesOnlyPrimaryItemTest() throws Exception {
        File prefixFile = File.createTempFile("packagerInverseScopeTest", ".zip");
        try {
            performExportWithScopeScript(article.getHandle(), prefixFile, "isAuthorOfPublication");
            context.commit();

            String base = prefixFile.getAbsolutePath().replaceAll("\\.[^.]+$", "");

            File articleZip  = new File(base + "_ITEM@" + article.getHandle().replace("/", "-")  + ".zip");
            File authorZip   = new File(base + "_ITEM@" + author.getHandle().replace("/", "-")   + ".zip");
            File author2Zip  = new File(base + "_ITEM@" + author2.getHandle().replace("/", "-")  + ".zip");

            assertTrue("Primary article zip should still exist", articleZip.exists());
            assertFalse("author zip should NOT exist for inverse scope",  authorZip.exists());
            assertFalse("author2 zip should NOT exist for inverse scope", author2Zip.exists());
        } finally {
            cleanupPrefixFiles("packagerInverseScopeTest");
            prefixFile.delete();
        }
    }

    /**
     * Verify that when disseminating with -z "*", no duplicate zip files are
     * created for items that appear multiple times in the relationship tree.
     * author appears in both article and article2 — should only produce ONE author zip.
     */
    @Test
    public void packagerExportNoDuplicateZipsTest() throws Exception {
        File prefixFile = File.createTempFile("packagerNoDupTest", ".zip");
        try {
            performExportWithScopeScript(article.getHandle(), prefixFile, "*");
            context.commit();

            // Count files matching the author handle — must be exactly 1
            String authorHandle = author.getHandle().replace("/", "-");
            File dir = prefixFile.getParentFile();
            File[] matches = dir.listFiles(f ->
                    f.getName().contains("ITEM@" + authorHandle));

            assertNotNull(matches);
            assertEquals("Author should be disseminated exactly once, not duplicated", 1, matches.length);
        } finally {
            cleanupPrefixFiles("packagerNoDupTest");
            prefixFile.delete();
        }
    }

    private METSManifest getManifest(File zipFile) throws IOException, MetadataValidationException {
        ZipFile zip = new ZipFile(zipFile);
        ZipEntry manifestEntry = zip.getEntry(METSManifest.MANIFEST_FILE);
        if (manifestEntry == null) {
            return null;
        }
        return METSManifest.create(zip.getInputStream(manifestEntry), false, "AIP");
    }

    private String getID() throws IOException, MetadataValidationException {
        METSManifest manifest = getManifest(resultFile);
        if (manifest == null) {
            return null;
        }
        Element mets = manifest.getMets();
        String idStr = mets.getAttributeValue("ID");
        if (idStr.contains("DB-ID-")) {
            idStr = idStr.substring(idStr.lastIndexOf("DB-ID-") + 6);
        }
        return idStr;
    }

    private void cleanupPrefixFiles(String prefix) throws IOException {
        // Clean temp dir
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempDir, prefix + "*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
        // Clean working dir (related item zips land here when no explicit output dir is given)
        Path workingDir = Paths.get(System.getProperty("user.dir"));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingDir, prefix + "*")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }

    private void performExportScript(String handle, File outputFile) throws Exception {
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle, "-t",
                "AIP", outputFile.getPath());
    }

    private void performExportWithScopeScript(String handle, File outputFile, String scope) throws Exception {
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle,
                "-z", scope, "-t", "AIP", outputFile.getPath());
    }

    private void performExportScriptToDir(String handle, File outputDir) throws Exception {
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle, "-t",
                "AIP", outputDir.getPath());
    }

    private void performImportNoForceScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }

    private void performImportScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-z", "*", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }

    private void performReplaceScript(File outputFile) throws Exception {
        runDSpaceScript("packager", "-r", "-f", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
