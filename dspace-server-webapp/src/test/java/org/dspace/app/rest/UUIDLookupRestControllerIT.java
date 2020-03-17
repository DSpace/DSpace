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

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.builder.BitstreamBuilder;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.SiteBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.eperson.Group;
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
     * Test the proper redirection of a site's uuid
     *
     * @throws Exception
     */
    public void testSiteUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a site to get the uuid to lookup
        Site site = SiteBuilder.createSite(context).build();

        context.restoreAuthSystemState();

        String uuid = site.getID().toString();
        String siteDetail = REST_SERVER_URL + "core/sites/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the site details
                        .andExpect(header().string("Location", siteDetail));
    }

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
    @Ignore
    /**
     * Test the proper redirection of a bundle's uuid
     *
     * @throws Exception
     */
    public void testBundleUUID() throws Exception {
        // Currently, there is no bundle endpoint
    }

    @Test
    /**
     * Test the proper redirection of uuids of different kinds of bitstreams (standard, collection and community logo)
     *
     * @throws Exception
     */
    public void testBitstreamUUID() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a community and a collection to get the uuid to lookup
        Community community = CommunityBuilder.createCommunity(context)
                                          .withName("A Community")
                                          .withLogo("Community Logo")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                            .withName("A Collection")
                                            .withLogo("Collection Logo")
                                            .build();

        Item item = ItemBuilder.createItem(context, collection)
                            .withTitle("An Item")
                            .build();

        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream("bitstreamContent", CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                        .withName("Bitstream")
                                        .withDescription("description")
                                        .withMimeType("text/plain")
                                        .build();
        }

        context.restoreAuthSystemState();

        String uuid = bitstream.getID().toString();
        String colLogoUuid = community.getLogo().getID().toString();
        String comLogoUuid = collection.getLogo().getID().toString();
        String bitstreamDetail = REST_SERVER_URL + "core/bitstreams/" + uuid;
        String colLogoDetail = REST_SERVER_URL + "core/bitstreams/" + colLogoUuid;
        String comLogoDetail = REST_SERVER_URL + "core/bitstreams/" + comLogoUuid;

        // test the resolution of a standard bitstream
        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", bitstreamDetail));

        // test the resolution of a community's logo bitstream
        getClient().perform(get("/api/dso/find?uuid={uuid}", comLogoUuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", comLogoDetail));

        // test the resolution of a collection's logo bitstream
        getClient().perform(get("/api/dso/find?uuid={uuid}", colLogoUuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the community details
                        .andExpect(header().string("Location", colLogoDetail));

    }

    @Test
    /**
     * Test the proper redirection of an eperson's uuid
     *
     * @throws Exception
     */
    public void testEPersonUUID() throws Exception {
        String uuid = eperson.getID().toString();
        String epersonDetail = REST_SERVER_URL + "eperson/epersons/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the eperson details
                        .andExpect(header().string("Location", epersonDetail));
    }

    @Test
    /**
     * Test the proper redirection of an eperson's uuid
     *
     * @throws Exception
     */
    public void testGroupUUID() throws Exception {
        // We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        Group group = GroupBuilder.createGroup(context)
                .withName("Test Group")
                .build();

        context.restoreAuthSystemState();

        String uuid = group.getID().toString();
        String groupDetail = REST_SERVER_URL + "eperson/groups/" + uuid;

        getClient().perform(get("/api/dso/find?uuid={uuid}",uuid))
                        .andExpect(status().isFound())
                        //We expect a Location header to redirect to the group details
                        .andExpect(header().string("Location", groupDetail));
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
     * Test that a request with an uuid parameter that is not an actual UUID return a 400 Bad Request status
     *
     * @throws Exception
     */
    public void testInvalidUUID() throws Exception {
        getClient().perform(get("/api/dso/find?uuid={uuid}","invalidUUID"))
                        .andExpect(status().isBadRequest());
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
