/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deletionprocess;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.deletion.process.DSpaceObjectDeletionProcess.OBJECT_DELETION_SCRIPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
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
import org.dspace.content.service.ItemService;
import org.dspace.deletion.process.DSpaceObjectDeletionProcess;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles ITs for { @link DSpaceObjectDeletionProcess }.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class DSpaceObjectDeletionProcessIT extends AbstractControllerIntegrationTest {

    private Item item1;
    private Item item2;
    private Community community;
    private Bitstream bitstream1;
    private Bitstream bitstream2;
    private Bitstream bitstream3;
    private Bitstream bitstream4;
    private Bitstream bitstream5;
    private Bitstream bitstream6;
    private Collection collection;

    @Autowired
    private ItemService itemService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context)
                                    .withName("My community")
                                    .build();
        collection = CollectionBuilder.createCollection(context, community)
                                      .withName("Publication collection")
                                      .withEntityType("Publication")
                                      .build();

        item1 = ItemBuilder.createItem(context, collection)
                           .withTitle("Publication item TEST 1")
                           .withType("TEST")
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

        context.restoreAuthSystemState();
    }

    @Test
    public void asyncDetetionOfItemTest() throws Exception {
        // verify that item with bitstreams exist
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream3.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream3.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream4.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream4.getName())));

        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", item1.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // // verify that item with bitstreams was deleted
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isNotFound());

        // check item2
        getClient(tokenAdmin).perform(get("/api/core/items/" + item2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream5.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream5.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream5.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream6.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream6.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream6.getName())));
    }

    @Test
    public void asyncDetetionOfCollectionTest() throws Exception {

        // verify that collection with items/bitstreams exists
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())));

        // check item1
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream3.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream3.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream4.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream4.getName())));

        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())));

        // check item2
        getClient(tokenAdmin).perform(get("/api/core/items/" + item2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream5.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream5.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream5.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream6.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream6.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream6.getName())));

        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", collection.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // // verify that collection with items/bitstreams was deleted
        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + item2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream5.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream6.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void asyncDetetionOfCommunityTest() throws Exception {

        // verify that community with collections/items/bitstreams exists
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/communities/" + community.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(community.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(community.getName())));

        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())));

        // check item1
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream3.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream3.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream4.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream4.getName())));

        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())));

        // check item2
        getClient(tokenAdmin).perform(get("/api/core/items/" + item2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream5.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream5.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream5.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream6.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream6.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream6.getName())));

        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", community.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // // verify that collection with items/bitstreams was deleted
        getClient(tokenAdmin).perform(get("/api/core/communities/" + community.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/collections/" + collection.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/items/" + item2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream5.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream6.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void asyncDetetionOfUnsupportedObjectTest() throws Exception {

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));

        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", bitstream1.getID().toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        var message = String.format("DSpaceObject for provided identifier:%s doesn't exist!", bitstream1.getID());
        assertTrue(handler.getException().getMessage().contains(message));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));
    }

    @Test
    public void asyncDetetionOfItemByHandleTest() throws Exception {
        // verify that item with bitstreams exist
        AtomicReference<String> idRef = new AtomicReference<>();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(item1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(item1.getName())))
                             .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(),
                            "$.handle")));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream1.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream1.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream2.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream2.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream3.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream3.getName())));

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(bitstream4.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(bitstream4.getName())));

        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", idRef.get() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // verify that item with bitstreams was deleted
        getClient(tokenAdmin).perform(get("/api/core/items/" + item1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAdmin).perform(get("/api/core/bitstreams/" + bitstream4.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void asyncDeletionWithNonExistentUUIDTest() throws Exception {
        UUID fakeUuid = UUID.randomUUID();
        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", fakeUuid.toString() };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        var expectedMessage = String.format("DSpaceObject for provided identifier:%s doesn't exist!", fakeUuid);
        assertTrue(handler.getException().getMessage().contains(expectedMessage));
    }

    @Test
    public void asyncDeletionWithInvalidHandleTest() throws Exception {
        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", "123456789/invalid" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        assertTrue(handler.getException() instanceof IllegalArgumentException);
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
    public void asyncDeletionOfItemWithCopyVirtualMetadataAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create entity types and relationship type
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();

        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, personEntityType,"isAuthorOfPublication", "isPublicationOfAuthor",0, null, 0,null
        ).withCopyToLeft(true)
         .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, community)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, community)
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
        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", personItem.getID().toString(), "-c", "all" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // Verify that person item was deleted
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + personItem.getID()))
                             .andExpect(status().isNotFound());

        // Verify that publication item still exists
        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", Matchers.is(publicationItem.getID().toString())));

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
    public void asyncDeletionOfItemWithCopyVirtualMetadataConfiguredTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create entity types
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        // Create relationship type with copyToRight=true (person will receive metadata from publication)
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, personEntityType, "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0,null
        ).withCopyToRight(true)
         .build();

        // Create relationship type with copyToRight=false (project will NOT receive metadata)
        RelationshipType isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, projectEntityType, "isProjectOfPublication", "isPublicationOfProject",
              0, null, 0, null
        ).withCopyToRight(false)
         .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, community)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, community)
                                                       .withName("Person collection")
                                                       .withEntityType("Person")
                                                       .build();
        Collection projectCollection = CollectionBuilder.createCollection(context, community)
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
        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", publicationItem.getID().toString(),
                                                              "-c", "configured" };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        // Verify that publication item was deleted
        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
                             .andExpect(status().isNotFound());

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
    public void asyncDeletionOfItemWithCopyVirtualMetadataByRelationshipTypeIdsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // Create entity types
        EntityType personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnitEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        // Create three relationship types (all with copyToRight=false to ensure IDs override config)
        RelationshipType isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, personEntityType, "isAuthorOfPublication", "isPublicationOfAuthor",
              0, null, 0, null
        ).withCopyToRight(false)
         .build();

        RelationshipType isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, projectEntityType, "isProjectOfPublication", "isPublicationOfProject",
              0, null, 0, null
        ).withCopyToRight(false)
         .build();

        RelationshipType isOrgUnitOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(context,
              publicationEntityType, orgUnitEntityType, "isOrgUnitOfPublication", "isPublicationOfOrgUnit",
              0, null, 0, null
        ).withCopyToRight(false)
         .build();

        Collection publicationCollection = CollectionBuilder.createCollection(context, community)
                                                            .withName("Publication collection")
                                                            .withEntityType("Publication")
                                                            .build();
        Collection personCollection = CollectionBuilder.createCollection(context, community)
                                                       .withName("Person collection")
                                                       .withEntityType("Person")
                                                       .build();
        Collection projectCollection = CollectionBuilder.createCollection(context, community)
                                                        .withName("Project collection")
                                                        .withEntityType("Project")
                                                        .build();
        Collection orgUnitCollection = CollectionBuilder.createCollection(context, community)
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
        String[] args = new String[]{ OBJECT_DELETION_SCRIPT, "-i", publicationItem.getID().toString(),
                                                              "-c", relationshipTypeIds };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        DSpaceObjectDeletionProcess deletionProcess = new DSpaceObjectDeletionProcess();
        deletionProcess.initialize(args, handler, admin);
        deletionProcess.run();

        // Verify that publication item was deleted
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/items/" + publicationItem.getID()))
                             .andExpect(status().isNotFound());

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