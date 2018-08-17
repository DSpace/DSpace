/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Integration test for the UUIDLookup endpoint
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class UUIDLookupRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    /**
     * Test the proper redirection of an community's uuid
     * 
     * @throws Exception
     */
    public void testCommunityUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a community to get the uuid to lookup
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("A Community")
                                          .build();

        context.restoreAuthSystemState();

        String uuid = community.getID().toString();
        String communityDetail = REST_SERVER_URL + "core/communities/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", communityDetail));
    }

    @Test
    /**
     * Test the proper redirection of an collection's uuid
     *
     * @throws Exception
     */
    public void testCollectionUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a community and a collection to get the uuid to lookup
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("A Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                            .withName("A Collection")
                                            .build();

        context.restoreAuthSystemState();

        String uuid = collection.getID().toString();
        String collectionDetail = REST_SERVER_URL + "core/collections/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", collectionDetail));
    }

    @Test
    /**
     * Test the proper redirection of an item's uuid
     *
     * @throws Exception
     */
    public void testItemUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a community and a collection to get the uuid to lookup
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("A Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                            .withName("A Collection")
                                            .build();

        Item item = ItemBuilder.createItem(context, collection)
                            .withTitle("An Item")
                            .build();

        context.restoreAuthSystemState();

        String uuid = item.getID().toString();
        String itemDetail = REST_SERVER_URL + "core/items/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", itemDetail));
    }

    @Test
    /**
     * Test that a request with a valid, not existent, uuid parameter return a 404 Not Found status
     *
     * @throws Exception
     */
    public void testUnexistentUUID() throws Exception {
        getClient().perform(get("/api/dso/find?uuid={uuid}",UUID.randomUUID().toString()))
                        .andExpect(status().isNotFound());
    }

    @Test
    /**
     * Test that a request with an uuid parameter that is not an actual UUID return a 422 Unprocessable Entity status
     *
     * @throws Exception
     */
    public void testInvalidUUID() throws Exception {
        getClient().perform(get("/api/dso/find?uuid={uuid}","invalidUUID"))
                        .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Ignore
    /**
     * This test will check the return status code when no uuid is supplied. It currently fails as our
     * RestResourceController take the precedence over the UUIDLookupController returning a 404 Repository not found
     *
     * @throws Exception
     */
    public void testMissingIdentifierParameter() throws Exception {
        getClient().perform(get("/api/dso/find"))
                        .andExpect(status().isUnprocessableEntity());
    }

}
