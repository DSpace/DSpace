/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageMappedItems authorization feature
 */
public class ManageMappedItemsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    private Community communityA;
    private Collection collectionA;
    private CollectionRest collectionARest;
    private Item itemA;
    private Bitstream bitstreamA;
    private BitstreamRest bitstreamARest;
    private Bundle bundleA;

    final String feature = "canManageMappedItems";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        communityA = CommunityBuilder.createCommunity(context)
            .withName("communityA")
            .build();
        collectionA = CollectionBuilder.createCollection(context, communityA)
            .withName("collectionA")
            .build();
        itemA = ItemBuilder.createItem(context, collectionA)
            .withTitle("itemA")
            .build();
        bundleA = BundleBuilder.createBundle(context, itemA)
            .withName("ORIGINAL")
            .build();
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamA = BitstreamBuilder.createBitstream(context, bundleA, is)
                .withName("bistreamA")
                .build();
        }

        context.restoreAuthSystemState();

        collectionARest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        bitstreamARest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
    }

    @Test
    public void adminCollectionAdminSuccess() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void epersonCollectionNotFound() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void addWriteEpersonCollectionSuccess() throws Exception {
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADD)
            .withUser(eperson)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.WRITE)
            .withUser(eperson)
            .build();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminEpersonCollectionSuccess() throws Exception {
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .withUser(eperson)
            .build();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousCollectionNotFound() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminBitstreamNotFound() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(bitstreamARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
