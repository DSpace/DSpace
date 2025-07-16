/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.deletionprocess;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.deletion.process.DSpaceObjectDeletionProcess;
import org.hamcrest.Matchers;
import org.junit.Test;

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

        String[] args = new String[]{"dspace-object-deletion", "-i", item1.getID().toString()};
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

        String[] args = new String[]{"dspace-object-deletion", "-i", collection.getID().toString()};
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

        String[] args = new String[]{"dspace-object-deletion", "-i", community.getID().toString()};
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

        String[] args = new String[]{ "dspace-object-deletion", "-i", bitstream1.getID().toString() };
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

        String[] args = new String[]{"dspace-object-deletion", "-i", idRef.get() };
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

}