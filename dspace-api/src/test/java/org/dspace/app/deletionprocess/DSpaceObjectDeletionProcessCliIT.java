/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deletionprocess;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link org.dspace.deletion.process.DSpaceObjectDeletionProcessCli}.
 * Tests CLI execution with the -e (email) parameter to specify different users.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcessCliIT extends AbstractIntegrationTestWithDatabase {


    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    private Community community;
    private Collection collection;
    private Item item1;
    private Item item2;
    private Bitstream bitstream1;
    private Bitstream bitstream2;
    private Bitstream bitstream3;
    private Bitstream bitstream4;
    private Bitstream bitstream5;
    private Bitstream bitstream6;

    private EPerson comAdmin;
    private EPerson colAdmin;
    private EPerson itemAdmin;
    private EPerson regularUser;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        // Create test users
        comAdmin = EPersonBuilder.createEPerson(context)
                                 .withEmail("comadmin@example.com")
                                 .withPassword(password)
                                 .build();

        colAdmin = EPersonBuilder.createEPerson(context)
                                 .withEmail("coladmin@example.com")
                                 .withPassword(password)
                                 .build();

        itemAdmin = EPersonBuilder.createEPerson(context)
                                  .withEmail("itemadmin@example.com")
                                  .withPassword(password)
                                  .build();

        regularUser = EPersonBuilder.createEPerson(context)
                                    .withEmail("regularuser@example.com")
                                    .withPassword(password)
                                    .build();

        // Create test hierarchy
        community = CommunityBuilder.createCommunity(context)
                                    .withName("Test Community")
                                    .withAdminGroup(comAdmin)
                                    .build();

        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Publication collection")
                                      .withEntityType("Publication")
                                      .withAdminGroup(colAdmin)
                                      .build();

        item1 = ItemBuilder.createItem(context, collection)
                           .withTitle("Publication item TEST 1")
                           .withType("TEST")
                           .withAdminUser(itemAdmin)
                           .build();

        item2 = ItemBuilder.createItem(context, collection)
                           .withTitle("Publication title test")
                           .withAuthor("Misha, Boychuk")
                           .withType("website_content")
                           .build();

        Bundle bundleOfItem1 = BundleBuilder.createBundle(context, item1)
                                            .withName("ORIGINAL")
                                            .build();

        try (InputStream is = IOUtils.toInputStream("Dummy content 1", CharEncoding.UTF_8)) {
            bitstream1 = BitstreamBuilder.createBitstream(context, bundleOfItem1, is)
                                         .withName("bitstream 1")
                                         .build();
        }

        try (InputStream is = IOUtils.toInputStream("Dummy content 2", CharEncoding.UTF_8)) {
            bitstream2 = BitstreamBuilder.createBitstream(context, bundleOfItem1, is)
                                         .withName("bitstream 2")
                                         .build();
        }

        try (InputStream is = IOUtils.toInputStream("Dummy content 3", CharEncoding.UTF_8)) {
            bitstream3 = BitstreamBuilder.createBitstream(context, bundleOfItem1, is)
                                         .withName("bitstream 3")
                                         .build();
        }

        try (InputStream is = IOUtils.toInputStream("Dummy content 4", CharEncoding.UTF_8)) {
            bitstream4 = BitstreamBuilder.createBitstream(context, bundleOfItem1, is)
                                         .withName("bitstream 4")
                                         .build();
        }

        Bundle bundleOfItem2 = BundleBuilder.createBundle(context, item2)
                                            .withName("ORIGINAL")
                                            .build();

        try (InputStream is = IOUtils.toInputStream("TEST 1", CharEncoding.UTF_8)) {
            bitstream5 = BitstreamBuilder.createBitstream(context, bundleOfItem2, is)
                                         .withName("test title 1 item2")
                                         .build();
        }

        try (InputStream is = IOUtils.toInputStream("TEST 2", CharEncoding.UTF_8)) {
            bitstream6 = BitstreamBuilder.createBitstream(context, bundleOfItem2, is)
                                         .withName("test title 2 item2")
                                         .build();
        }

        context.commit();
        context.restoreAuthSystemState();
    }

    /**
     * Test deletion of an item with admin user specified via -e parameter.
     * Verifies that item and associated bitstreams are properly deleted.
     */
    @Test
    public void cliDeletionOfItemWithAdminTest() throws Exception {
        runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", item1.getID().toString());
        context.commit();

        // verify that item with bitstreams was deleted
        assertThat("Item expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream1.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream2.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream3.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream4.getID()).isDeleted(), is(true));

        // verify item2 still exists
        Item item2Found = itemService.find(context, item2.getID());
        assertThat("Item expected to exist", item2Found, notNullValue());
        assertThat(item2Found.getName(), is(item2.getName()));

        Bitstream bitstream5Found = bitstreamService.find(context, bitstream5.getID());
        assertThat("Bitstream expected to exist", bitstream5Found, notNullValue());
        assertThat(bitstream5Found.getName(), is(bitstream5.getName()));
        assertThat("Bitstream expected to NOT be deleted", bitstream5Found.isDeleted(), is(false));

        Bitstream bitstream6Found = bitstreamService.find(context, bitstream6.getID());
        assertThat("Bitstream expected to exist", bitstream6Found, notNullValue());
        assertThat(bitstream6Found.getName(), is(bitstream6.getName()));
        assertThat("Bitstream expected to NOT be deleted", bitstream6Found.isDeleted(), is(false));
    }

    /**
     * Test deletion of a collection with admin user specified via -e parameter.
     * Verifies cascading deletion of collection, items, and bitstreams.
     */
    @Test
    public void cliDeletionOfCollectionWithAdminTest() throws Exception {
        // verify that collection with items/bitstreams exists
        assertThat("Collection expected to exist", collectionService.find(context, collection.getID()), notNullValue());
        assertThat("Item expected to exist", itemService.find(context, item1.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream1.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream2.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream3.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream4.getID()), notNullValue());
        assertThat("Item expected to exist", itemService.find(context, item2.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream5.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream6.getID()), notNullValue());

        runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", collection.getID().toString());
        context.commit();

        // verify that collection with items/bitstreams was deleted
        assertThat("Collection expected to be deleted", collectionService.find(context, collection.getID()),
                   nullValue());
        assertThat("Item expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Item expected to be deleted", itemService.find(context, item2.getID()), nullValue());
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream1.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream2.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream3.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream4.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream5.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream6.getID()).isDeleted(), is(true));
    }

    /**
     * Test that deletion of unsupported objects (like bitstreams) fails properly.
     */
    @Test
    public void cliDeletionOfUnsupportedObjectTest() throws Exception {
        // verify that bitstream exists
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream1.getID()), notNullValue());

        try {
            runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", bitstream1.getID().toString());
            fail("Expected IllegalArgumentException for unsupported object type");
        } catch (IllegalArgumentException e) {
            var expectedMessage = String.format("DSpaceObject for provided identifier:%s doesn't exist!",
                                                bitstream1.getID());
            assertTrue(e.getMessage().contains(expectedMessage));
        }

        // verify that bitstream still exists (not deleted)
        assertThat("Bitstream expected to still exist", bitstreamService.find(context, bitstream1.getID()),
                   notNullValue());
    }

    /**
     * Test deletion of an item by handle with admin user specified via -e parameter.
     */
    @Test
    public void cliDeletionOfItemByHandleTest() throws Exception {
        // verify that item with bitstreams exist
        assertThat("Item expected to exist", itemService.find(context, item1.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream1.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream2.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream3.getID()), notNullValue());
        assertThat("Bitstream expected to exist", bitstreamService.find(context, bitstream4.getID()), notNullValue());

        runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", item1.getHandle());
        context.commit();

        // verify that item with bitstreams was deleted
        assertThat("Item expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream1.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream2.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream3.getID()).isDeleted(), is(true));
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream4.getID()).isDeleted(), is(true));
    }

    /**
     * Test that CLI deletion works with item admin user specified via -e parameter.
     */
    @Test
    public void cliDeletionWithItemAdminUserTest() throws Exception {
        runDSpaceScript("object-deletion", "-e", itemAdmin.getEmail(), "-i", item1.getID().toString());
        context.commit();

        // Verify deletion was successful
        assertThat("Item expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Bitstream expected to be soft-deleted",
                   bitstreamService.find(context, bitstream1.getID()).isDeleted(), is(true));
    }

    /**
     * Test that CLI deletion works with collection admin user specified via -e parameter.
     */
    @Test
    public void cliDeletionWithCollectionAdminUserTest() throws Exception {
        runDSpaceScript("object-deletion", "-e", colAdmin.getEmail(), "-i", collection.getID().toString());
        context.commit();

        // Verify deletion was successful
        assertThat("Collection expected to be deleted",
                   collectionService.find(context, collection.getID()), nullValue());
        assertThat("Item1 expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Item2 expected to be deleted", itemService.find(context, item2.getID()), nullValue());
        assertThat("Bitstream1 expected to be soft-deleted",
                   bitstreamService.find(context, bitstream1.getID()).isDeleted(), is(true));
        assertThat("Bitstream2 expected to be soft-deleted",
                   bitstreamService.find(context, bitstream2.getID()).isDeleted(), is(true));
    }

    /**
     * Test that CLI deletion works with community admin user specified via -e parameter.
     */
    @Test
    public void cliDeletionWithCommunityAdminUserTest() throws Exception {
        runDSpaceScript("object-deletion", "-e", comAdmin.getEmail(), "-i", community.getID().toString());
        context.commit();

        // Verify deletion was successful
        assertThat("Community expected to be deleted",
                   communityService.find(context, community.getID()), nullValue());
        assertThat("Collection expected to be deleted",
                   collectionService.find(context, collection.getID()), nullValue());
        assertThat("Item1 expected to be deleted", itemService.find(context, item1.getID()), nullValue());
        assertThat("Item2 expected to be deleted", itemService.find(context, item2.getID()), nullValue());
    }

    /**
     * Test that CLI deletion fails when regular user (non-admin) is specified.
     */
    @Test
    public void cliDeletionWithRegularUserFailsTest() throws Exception {
        try {
            runDSpaceScript("object-deletion", "-e", regularUser.getEmail(), "-i", item2.getID().toString());
            fail("Expected exception for unauthorized user");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("not eligible to execute script: 'object-deletion'"));
        }

        // Verify item was NOT deleted
        assertThat("Item expected to still exist", itemService.find(context, item2.getID()), notNullValue());
        assertThat("Bitstream expected to still exist and not be soft-deleted",
                   bitstreamService.find(context, bitstream5.getID()).isDeleted(), is(false));
    }

    /**
     * Test that CLI deletion fails when item admin tries to delete an item they don't administer.
     */
    @Test
    public void cliDeletionWithItemAdminOnUnauthorizedItemFailsTest() throws Exception {
        try {
            runDSpaceScript("object-deletion", "-e", itemAdmin.getEmail(),
                            "-i", item2.getID().toString());  // item2 is NOT administered by itemAdmin
            fail("Expected exception for unauthorized item");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("not eligible to execute script: 'object-deletion'"));
        }

        // Verify item was NOT deleted
        assertThat("Item expected to still exist", itemService.find(context, item2.getID()), notNullValue());
    }

    /**
     * Test that CLI deletion fails when -e parameter is missing.
     */
    @Test
    public void cliDeletionWithMissingEmailParameterFailsTest() throws Exception {
        try {
            runDSpaceScript("object-deletion", "-i", item1.getID().toString());
            fail("Expected ParseException for missing -e parameter");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Required parameter -e missing!"));
        }

        // Verify item was NOT deleted
        assertThat("Item expected to still exist", itemService.find(context, item1.getID()), notNullValue());
    }

    /**
     * Test that CLI deletion fails when invalid email is provided.
     */
    @Test
    public void cliDeletionWithInvalidEmailFailsTest() throws Exception {
        try {
            runDSpaceScript("object-deletion", "-e", "nonexistent@example.com", "-i", item1.getID().toString());
            fail("Expected IllegalArgumentException for invalid email");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Unable to find a user with email: nonexistent@example.com"));
        }

        // Verify item was NOT deleted
        assertThat("Item expected to still exist", itemService.find(context, item1.getID()), notNullValue());
    }

    /**
     * Test that CLI deletion fails when non-existent UUID is provided.
     */
    @Test
    public void cliDeletionWithNonExistentUUIDTest() throws Exception {
        UUID fakeUuid = UUID.randomUUID();
        try {
            runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", fakeUuid.toString());
            fail("Expected IllegalArgumentException for non-existent UUID");
        } catch (Exception e) {
            var expectedMessage = String.format("DSpaceObject for provided identifier:%s doesn't exist!", fakeUuid);
            assertThat(e.getMessage(), containsString(expectedMessage));
        }
    }

    /**
     * Test that CLI deletion fails when invalid handle is provided.
     */
    @Test
    public void cliDeletionWithInvalidHandleTest() throws Exception {
        try {
            runDSpaceScript("object-deletion", "-e", admin.getEmail(), "-i", "123456789/invalid");
            fail("Expected IllegalArgumentException for invalid handle");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertThat(e.getMessage(), notNullValue());
        }
    }

    /**
     * Test deletion of an Item with the -c all (copyVirtualMetadata=all) option.
     * This test verifies that when deleting an Item with relationships, the virtual metadata
     * generated by those relationships is copied as physical metadata to the related items
     * before the deletion occurs.
     *
     * Case:
     * 1. Create a Person item and a Publication item with a relationship between them
     * 2. Verify the Publication has NO physical dc.contributor.author metadata (only virtual)
     * 3. Delete the Person item using -c all option
     * 4. Verify the Person is deleted
     * 5. Verify the Publication still exists AND now has physical dc.contributor.author metadata
     *
     * This ensures that the virtual metadata (e.g., "Smith, John") is preserved as permanent
     * physical metadata in the Publication item after the related Person item is deleted.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void cliDeletionOfItemWithCopyVirtualMetadataAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community reloadedCommunity = context.reloadEntity(community);

        // Create entity types and relationship type
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null
        ).withCopyToLeft(true).build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                       .withName("Person collection")
                                                       .withEntityType("Person")
                                                       .build();

        // Create Person item with metadata
        Item personItem = ItemBuilder.createItem(context, personCollection)
                                     .withTitle("John Smith")
                                     .withPersonIdentifierFirstName("John")
                                     .withPersonIdentifierLastName("Smith")
                                     .build();

        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withTitle("Quantum Computing Research")
                                          .build();

        // Create relationship between publication and person
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, personItem, isAuthorOfPublication)
                           .build();

        context.commit();
        context.restoreAuthSystemState();

        // Check that publication has NO physical dc.contributor.author metadata
        // (only virtual metadata from relationship should be visible via REST API)
        context.turnOffAuthorisationSystem();
        publicationItem = context.reloadEntity(publicationItem);
        List<MetadataValue> physicalAuthorMetadata = itemService.getMetadata(
            publicationItem, "dc", "contributor", "author", Item.ANY, false
        );
        assertTrue("Publication should have NO physical dc.contributor.author metadata initially",
                   physicalAuthorMetadata.isEmpty());
        context.restoreAuthSystemState();

        // Delete person item with -c all option
        runDSpaceScript("object-deletion", "-e", admin.getEmail(),
                "-i", personItem.getID().toString(), "-c", "all");
        context.commit();

        // Verify that person item was deleted
        assertThat("Person item expected to be deleted", itemService.find(context, personItem.getID()), nullValue());

        // Verify that publication item still exists
        assertThat("Publication item expected to still exist",
                   itemService.find(context, publicationItem.getID()), notNullValue());

        // Check that publication NOW HAS physical dc.contributor.author metadata
        // (copied from virtual metadata before deletion)
        context.turnOffAuthorisationSystem();
        publicationItem = context.reloadEntity(publicationItem);
        List<MetadataValue> physicalAuthorMetadataAfter = itemService.getMetadata(
            publicationItem, "dc", "contributor", "author", Item.ANY, false
        );
        assertFalse("Publication should have physical dc.contributor.author metadata after deletion with -c all",
                    physicalAuthorMetadataAfter.isEmpty());
        assertEquals("Physical metadata should contain the copied author name", 1, physicalAuthorMetadataAfter.size());
        String authorValue = physicalAuthorMetadataAfter.get(0).getValue();
        assertTrue("Author metadata should contain 'Smith'", authorValue.contains("Smith"));
        assertTrue("Author metadata should contain 'John'", authorValue.contains("John"));
        context.restoreAuthSystemState();
    }

    /**
     * Test deletion of an Item with the -c configured (copyVirtualMetadata=configured) option.
     * This test verifies that when deleting an Item with multiple relationships, only the virtual
     * metadata from relationships configured with copyToLeft/copyToRight=true are copied as
     * physical metadata to the related items.
     *
     * Case:
     * 1. Create a Publication with two relationships: one with copyToRight=true, one with copyToRight=false
     * 2. Verify the related items (Person, Project) have NO physical metadata initially
     * 3. Delete the Publication using -c configured option
     * 4. Verify the Publication is deleted
     * 5. Verify that ONLY the Person (copyToRight=true) received physical metadata
     * 6. Verify that the Project (copyToRight=false) did NOT receive physical metadata
     *
     * This ensures that only relationships configured to copy metadata actually do so when the
     * left item (Publication) is deleted.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void cliDeletionOfItemWithCopyVirtualMetadataConfiguredTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community reloadedCommunity = context.reloadEntity(community);

        // Create entity types and relationship type
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        // Create relationship type with copyToRight=true (person will receive metadata from publication)
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null
        ).withCopyToRight(true).build();

        // Create relationship type with copyToRight=false (project will NOT receive metadata)
        RelationshipType isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null
        ).withCopyToRight(false).build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                       .withName("Person collection")
                                                       .withEntityType("Person")
                                                       .build();
        Collection projectCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                        .withName("Project collection")
                                                        .withEntityType("Project")
                                                        .build();

        // Create Publication item with title
        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withTitle("Advanced Quantum Research")
                                          .build();

        // Create Person item
        Item personItem = ItemBuilder.createItem(context, personCollection)
                                     .withTitle("Jane Doe")
                                     .withPersonIdentifierFirstName("Jane")
                                     .withPersonIdentifierLastName("Doe")
                                     .build();

        // Create Project item
        Item projectItem = ItemBuilder.createItem(context, projectCollection)
                                      .withTitle("Quantum Computing Project")
                                      .build();

        // Create relationships
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, personItem, isAuthorOfPublication)
                           .build();
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, projectItem, isProjectOfPublication)
                           .build();

        context.commit();
        context.restoreAuthSystemState();

        // Check that person has NO physical relation.isPublicationOfAuthor metadata
        context.turnOffAuthorisationSystem();
        personItem = context.reloadEntity(personItem);
        List<MetadataValue> personPhysicalMetadata = itemService.getMetadata(
            personItem, "relation", "isPublicationOfAuthor", null, Item.ANY, false
        );
        assertTrue("Person should have NO physical relation.isPublicationOfAuthor metadata initially",
                   personPhysicalMetadata.isEmpty());

        // Check that project has NO physical relation.isPublicationOfProject metadata
        projectItem = context.reloadEntity(projectItem);
        List<MetadataValue> projectPhysicalMetadata = itemService.getMetadata(
            projectItem, "relation", "isPublicationOfProject", null, Item.ANY, false
        );
        assertTrue("Project should have NO physical relation.isPublicationOfProject metadata initially",
                   projectPhysicalMetadata.isEmpty());
        context.restoreAuthSystemState();

        // Delete publication item with -c configured option
        runDSpaceScript("object-deletion", "-e", admin.getEmail(),
                       "-i", publicationItem.getID().toString(), "-c", "configured");
        context.commit();

        // Verify that publication item was deleted
        assertThat("Publication item expected to be deleted",
                   itemService.find(context, publicationItem.getID()), nullValue());

        // Check that person NOW HAS physical relation.isPublicationOfAuthor metadata (copyToRight=true)
        context.turnOffAuthorisationSystem();
        personItem = context.reloadEntity(personItem);
        List<MetadataValue> personPhysicalMetadataAfter = itemService.getMetadata(
            personItem, "relation", "isPublicationOfAuthor", null, Item.ANY, false
        );
        assertFalse("Person should have physical relation.isPublicationOfAuthor metadata after deletion with " +
                        "-c configured (copyToRight=true)",
                    personPhysicalMetadataAfter.isEmpty());
        assertEquals("Person should have one relation metadata value", 1, personPhysicalMetadataAfter.size());
        assertEquals("Person metadata should reference the deleted publication UUID",
                     publicationItem.getID().toString(),
                     personPhysicalMetadataAfter.get(0).getValue());

        // Check that project STILL HAS NO physical relation.isPublicationOfProject metadata (copyToRight=false)
        projectItem = context.reloadEntity(projectItem);
        List<MetadataValue> projectPhysicalMetadataAfter = itemService.getMetadata(
            projectItem, "relation", "isPublicationOfProject", null, Item.ANY, false
        );
        assertTrue("Project should NOT have physical relation.isPublicationOfProject metadata after deletion " +
                       "with -c configured (copyToRight=false)",
                   projectPhysicalMetadataAfter.isEmpty());
        context.restoreAuthSystemState();
    }

    /**
     * Test deletion of an Item with the -c option using specific RelationshipType IDs.
     * This test verifies that when deleting an Item with multiple relationships, only the virtual
     * metadata from the specified RelationshipType IDs are copied as physical metadata to the
     * related items, regardless of the copyToLeft/copyToRight configuration.
     *
     * Case:
     * 1. Create a Publication with three relationships (Person, Project, OrgUnit)
     * 2. Verify the related items have NO physical metadata initially
     * 3. Delete the Publication using -c with only the Person and OrgUnit relationship IDs
     * 4. Verify the Publication is deleted
     * 5. Verify that ONLY Person and OrgUnit received physical metadata (specified IDs)
     * 6. Verify that Project did NOT receive physical metadata (ID not specified)
     *
     * This ensures that the -c option with numeric IDs allows granular control over which
     * virtual metadata to copy, independent of the relationship type configuration.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void cliDeletionOfItemWithCopyVirtualMetadataByRelationshipTypeIdsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community reloadedCommunity = context.reloadEntity(community);

        // Create entity types
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        // Create three relationship types (all with copyToRight=false to ensure IDs override config)
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null
        ).withCopyToRight(false).build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null
        ).withCopyToRight(false).build();

        RelationshipType isOrgUnitOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, orgUnitEntityType,
                "isOrgUnitOfPublication", "isPublicationOfOrgUnit", 0, null, 0, null
        ).withCopyToRight(false).build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                       .withName("Person collection")
                                                       .withEntityType("Person")
                                                       .build();
        Collection projectCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                        .withName("Project collection")
                                                        .withEntityType("Project")
                                                        .build();
        Collection orgUnitCollection = CollectionBuilder.createCollection(context, reloadedCommunity)
                                                        .withName("OrgUnit collection")
                                                        .withEntityType("OrgUnit")
                                                        .build();

        Item publicationItem = ItemBuilder.createItem(context, publicationCollection)
                                          .withTitle("Comprehensive Study on AI")
                                          .build();

        Item personItem = ItemBuilder.createItem(context, personCollection)
                                     .withTitle("Alice Johnson")
                                     .withPersonIdentifierFirstName("Alice")
                                     .withPersonIdentifierLastName("Johnson")
                                     .build();

        Item projectItem = ItemBuilder.createItem(context, projectCollection)
                                      .withTitle("AI Research Initiative")
                                      .build();

        Item orgUnitItem = ItemBuilder.createItem(context, orgUnitCollection)
                                      .withTitle("Computer Science Department")
                                      .build();

        // Create relationships
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, personItem, isAuthorOfPublication)
                           .build();
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, projectItem, isProjectOfPublication)
                           .build();
        RelationshipBuilder.createRelationshipBuilder(context, publicationItem, orgUnitItem, isOrgUnitOfPublication)
                           .build();

        context.commit();
        context.restoreAuthSystemState();

        // Check that person has NO physical relation.isPublicationOfAuthor metadata
        context.turnOffAuthorisationSystem();
        personItem = context.reloadEntity(personItem);
        List<MetadataValue> personPhysicalMetadata = itemService.getMetadata(
            personItem, "relation", "isPublicationOfAuthor", null, Item.ANY, false
        );
        assertTrue("Person should have NO physical relation.isPublicationOfAuthor metadata initially",
                   personPhysicalMetadata.isEmpty());

        // Check that project has NO physical relation.isPublicationOfProject metadata
        projectItem = context.reloadEntity(projectItem);
        List<MetadataValue> projectPhysicalMetadata = itemService.getMetadata(
            projectItem, "relation", "isPublicationOfProject", null, Item.ANY, false
        );
        assertTrue("Project should have NO physical relation.isPublicationOfProject metadata initially",
                   projectPhysicalMetadata.isEmpty());

        // Check that orgUnit has NO physical relation.isPublicationOfOrgUnit metadata
        orgUnitItem = context.reloadEntity(orgUnitItem);
        List<MetadataValue> orgUnitPhysicalMetadata = itemService.getMetadata(
            orgUnitItem, "relation", "isPublicationOfOrgUnit", null, Item.ANY, false
        );
        assertTrue("OrgUnit should have NO physical relation.isPublicationOfOrgUnit metadata initially",
                   orgUnitPhysicalMetadata.isEmpty());
        context.restoreAuthSystemState();

        // Delete publication item with -c specifying only Person and OrgUnit relationship type IDs
        // We exclude the Project relationship type ID
        String relationshipTypeIds = isAuthorOfPublication.getID() + "," + isOrgUnitOfPublication.getID();
        runDSpaceScript("object-deletion", "-e", admin.getEmail(),
                       "-i", publicationItem.getID().toString(), "-c", relationshipTypeIds);
        context.commit();

        // Verify that publication item was deleted
        assertThat("Publication item expected to be deleted",
                   itemService.find(context, publicationItem.getID()), nullValue());

        // Check that person NOW HAS physical relation.isPublicationOfAuthor metadata (ID was specified)
        context.turnOffAuthorisationSystem();
        personItem = context.reloadEntity(personItem);
        List<MetadataValue> personPhysicalMetadataAfter = itemService.getMetadata(
            personItem, "relation", "isPublicationOfAuthor", null, Item.ANY, false
        );
        assertFalse("Person should have physical relation.isPublicationOfAuthor metadata after deletion " +
                        "with -c using its relationship type ID",
                    personPhysicalMetadataAfter.isEmpty());
        assertEquals("Person should have one relation metadata value", 1, personPhysicalMetadataAfter.size());
        assertEquals("Person metadata should reference the deleted publication UUID",
                     publicationItem.getID().toString(),
                     personPhysicalMetadataAfter.get(0).getValue());

        // Check that orgUnit NOW HAS physical relation.isPublicationOfOrgUnit metadata (ID was specified)
        orgUnitItem = context.reloadEntity(orgUnitItem);
        List<MetadataValue> orgUnitPhysicalMetadataAfter = itemService.getMetadata(
            orgUnitItem, "relation", "isPublicationOfOrgUnit", null, Item.ANY, false
        );
        assertFalse("OrgUnit should have physical relation.isPublicationOfOrgUnit metadata after deletion " +
                        "with -c using its relationship type ID",
                    orgUnitPhysicalMetadataAfter.isEmpty());
        assertEquals("OrgUnit should have one relation metadata value", 1, orgUnitPhysicalMetadataAfter.size());
        assertEquals("OrgUnit metadata should reference the deleted publication UUID",
                     publicationItem.getID().toString(),
                     orgUnitPhysicalMetadataAfter.get(0).getValue());

        // Check that project STILL HAS NO physical relation.isPublicationOfProject metadata (ID was NOT specified)
        projectItem = context.reloadEntity(projectItem);
        List<MetadataValue> projectPhysicalMetadataAfter = itemService.getMetadata(
            projectItem, "relation", "isPublicationOfProject", null, Item.ANY, false
        );
        assertTrue("Project should NOT have physical relation.isPublicationOfProject metadata after deletion " +
                       "because its relationship type ID was not included in -c parameter",
                   projectPhysicalMetadataAfter.isEmpty());
        context.restoreAuthSystemState();
    }

}
