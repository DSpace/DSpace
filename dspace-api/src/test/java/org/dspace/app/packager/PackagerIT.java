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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Iterators;
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
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipMetadataService;
import org.dspace.content.RelationshipMetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
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
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected static final InstallItemService installItemService = ContentServiceFactory.getInstance()
            .getInstallItemService();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected RelationshipMetadataService relationshipMetadataService = ContentServiceFactory
            .getInstance().getRelationshipMetadataService();
    protected RelationshipService relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
    protected Community child1;
    protected Collection col1;
    protected Collection col2;
    protected Item article;
    protected Item author;
    protected Item author2;
    protected Item author3;
    protected Item author4;
    protected Item author5;
    protected Item author6;
    File tempFile;
    File resultFile;

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

        col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Authors")
                                .withEntityType("Person").build();

        // Create a new Publication (which is an Article)
        article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .build();

        author = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName")
                .build();

        author2 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("2")
                .withPersonIdentifierFirstName("firstName")
                .build();

        author3 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("3")
                .withPersonIdentifierFirstName("firstName")
                .build();

        author4 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("4")
                .withPersonIdentifierFirstName("firstName")
                .build();

        author5 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("5")
                .withPersonIdentifierFirstName("firstName")
                .build();

        author6 = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("6")
                .withPersonIdentifierFirstName("firstName")
                .build();

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
        super.destroy();
    }

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        performExportScript(article.getHandle(), tempFile);
        assertTrue(resultFile.length() > 0);
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
        performImportScript(resultFile);
        context.commit();
        Item item = itemService.find(context, UUID.fromString(idStr));
        assertNotNull(item);
    }

    @Test
    public void packagerImportColUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configService.setProperty("upload.temp.dir",tempFile.getParent());

        performExportScript(col1.getHandle(), tempFile);
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        resultFile = new File(path + "_COLLECTION@" +
                col1.getHandle().replace("/", "-") + ".zip");
        String idStr = getID();
        collectionService.delete(context, col1);
        performImportScript(resultFile);
        context.commit();
        Collection collection = collectionService.find(context, UUID.fromString(idStr));
        assertNotNull(collection);
    }

    @Test
    public void packagerImportComUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configService.setProperty("upload.temp.dir",tempFile.getParent());

        //Community
        performExportScript(child1.getHandle(), tempFile);
        String path = tempFile.getAbsolutePath().split("\\.")[0];
        resultFile = new File(path + "_COMMUNITY@" +
                child1.getHandle().replace("/", "-") + ".zip");
        String idStr = getID();
        communityService.delete(context, child1);
        performImportScript(resultFile);
        context.commit();
        Community community = communityService.find(context, UUID.fromString(idStr));
        assertNotNull(community);
    }

    @Test
    public void packagerUUIDAlreadyExistTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //Item should be overwritten if UUID already Exists
        performExportScript(article.getHandle(), tempFile);
        performImportScript(resultFile);
        context.commit();
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
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, col1, id).build();
        installItemService.installItem(context, workspaceItem, "123456789/0100");
        performImportNoForceScript(resultFile);
        Iterator<Item> items = itemService.findByCollection(context, col1);
        Item testItem = items.next();
        assertFalse(items.hasNext()); //check to make sure there is only 1 item
        assertEquals("123456789/0100", testItem.getHandle()); //check to make sure the item wasn't overwritten as
        // it would have the old handle.
        itemService.delete(context, testItem);
    }

    @Test
    public void packagerImportRelationshipTest() throws Exception {
        //Tests if a import will restore the relationship of an item
        context.turnOffAuthorisationSystem();
        createRels();
        context.turnOffAuthorisationSystem();
        performExportScript(article.getHandle(), tempFile);
        // For some reason if you don't commit here relationshipBuilder.deleteRelationship fails because
        // relationshipService.find(context, id) says that the relationship doesn't exist
        context.commit();
        List<RelationshipMetadataValue> leftList = relationshipMetadataService
                .getRelationshipMetadata(article, true);
        assertThat(leftList.size(), equalTo(18));
        performImportScript(resultFile);
        //get the new item create by the import
        Item item2 = itemService.findByIdOrLegacyId(context, article.getID().toString());
        leftList = relationshipMetadataService
                .getRelationshipMetadata(item2, true);
        List<Relationship> relationships = relationshipService.findByItem(context, item2);
        List<MetadataValue> virtual = itemService.getMetadata(item2, "dc", "contributor", "author", Item.ANY, true);
        MetadataValue familyValue = null;

        for (MetadataValue value : virtual) {
            if (!value.getValue().equals("familyName, firstName")) {
                continue;
            }
            familyValue = value;
        }
        assertNotNull(familyValue);
        assertThat(familyValue.getValue(), equalTo("familyName, firstName"));
        assertThat(familyValue.getAuthority(), equalTo("virtual::7"));
        assertThat(relationships.size(), equalTo(6));
        assertThat(relationships.get(0).getLeftPlace(), equalTo(0));
        assertThat(relationships.get(0).getRightPlace(), equalTo(0));
        assertThat(itemService.getMetadataFirstValue(relationships.get(0).getRightItem(),
                "person", "familyName", null, Item.ANY), equalTo("familyName"));
        assertThat(itemService.getMetadataFirstValue(relationships.get(5).getRightItem(),
                "person", "familyName", null, Item.ANY), equalTo("6"));
        assertThat(relationships.get(5).getLeftPlace(), equalTo(5));
        assertThat(relationships.get(5).getRightPlace(), equalTo(0));

        //check to see if the metadata exists in the new item
        assertThat(leftList.size(), equalTo(18));
    }

    @Test
    public void packagerExportRelationshipTest() throws Exception {
        //Test sees if the mets created by the packager includes the metadatafields
        //required to do a relationship restore
        context.turnOffAuthorisationSystem();
        createRels();
        context.restoreAuthSystemState();
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
        context.commit();
    }

    protected void createRels() {
        // Created the entity types needed for creating relations and related them to the items created previously
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        RelationshipType isAuthorOfPublication =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        0, 10, 0, 10).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author, isAuthorOfPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author2, isAuthorOfPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author3, isAuthorOfPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author4, isAuthorOfPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author5, isAuthorOfPublication).build();
        RelationshipBuilder.createRelationshipBuilder(context, article, author6, isAuthorOfPublication).build();
    }

    private String getID() throws IOException, MetadataValidationException {
        // This method gets the UUID from the mets file that is stored in the attribute element
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
        runDSpaceScript("packager", "-r", "-f", "-u", "-e", "admin@email.com", "-t",
                "AIP", outputFile.getPath());
    }
}
