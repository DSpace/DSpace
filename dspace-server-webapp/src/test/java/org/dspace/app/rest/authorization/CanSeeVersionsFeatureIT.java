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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.AbstractBuilder;
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
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canSeeVersions authorization feature.
 * <p>
 * Shared test fixtures are created once on the first test's
 * {@code @BeforeEach} and reused across all tests via static UUID fields.
 * </p>
 */
public class CanSeeVersionsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    private Community communityA;
    private Collection collectionA;
    private Item itemA;
    private ItemRest itemARest;
    private Bitstream bitstreamA;
    private BitstreamRest bitstreamARest;
    private Bundle bundleA;

    final String feature = "canSeeVersions";

    // Static UUIDs for reloading shared fixtures
    private static UUID communityAId;
    private static UUID collectionAId;
    private static UUID itemAId;
    private static boolean sharedFixturesCreated = false;

    private static final Map<String, String> authTokenCache =
        new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        if (!sharedFixturesCreated) {
            context.turnOffAuthorisationSystem();

            communityA = CommunityBuilder.createCommunity(context)
                .withName("communityA")
                .build();
            collectionA = CollectionBuilder
                .createCollection(context, communityA)
                .withName("collectionA")
                .build();
            itemA = ItemBuilder.createItem(context, collectionA)
                .withTitle("itemA")
                .build();

            context.restoreAuthSystemState();

            communityAId = communityA.getID();
            collectionAId = collectionA.getID();
            itemAId = itemA.getID();

            context.commit();
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            communityA = communityService.find(context,
                communityAId);
            collectionA = collectionService.find(context,
                collectionAId);
            itemA = itemService.find(context, itemAId);
        }

        eperson = context.reloadEntity(eperson);

        // Create bundle and bitstream per test (cleaned up by
        // AbstractBuilder after each test)
        context.turnOffAuthorisationSystem();

        // Restore default policies on the shared item. Tests that
        // call removeAllPolicies() strip policies that subsequent
        // tests depend on.
        authorizeService.removeAllPolicies(context, itemA);
        itemService.inheritCollectionDefaultPolicies(
            context, itemA, collectionA);
        bundleA = BundleBuilder.createBundle(context, itemA)
            .withName("ORIGINAL")
            .build();
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(
                bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamA = BitstreamBuilder
                .createBitstream(context, bundleA, is)
                .withName("bistreamA")
                .build();
        }
        context.restoreAuthSystemState();

        itemARest = itemConverter.convert(itemA, Projection.DEFAULT);
        bitstreamARest = bitstreamConverter.convert(
            bitstreamA, Projection.DEFAULT);
    }

    /**
     * Clean up shared fixtures after all tests have completed.
     */
    @AfterAll
    public static void tearDownSharedFixtures() throws Exception {
        if (!sharedFixturesCreated) {
            return;
        }
        Context ctx = new Context(Context.Mode.READ_WRITE);
        ctx.turnOffAuthorisationSystem();

        CommunityService comService =
            ContentServiceFactory.getInstance()
                .getCommunityService();
        Community com = comService.find(ctx, communityAId);
        if (com != null) {
            comService.delete(ctx, com);
        }

        ctx.complete();
        sharedFixturesCreated = false;
        authTokenCache.clear();
    }

    private String getCachedAuthToken(String email)
        throws Exception {
        String token = authTokenCache.get(email);
        if (token == null) {
            token = getAuthToken(email, password);
            authTokenCache.put(email, token);
        }
        return token;
    }

    @Test
    public void anonymousItemSuccess() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void epersonItemSuccess() throws Exception {
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminItemSuccess() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousPrivateItemNotFound() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void epersonPrivateItemNotFound() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminPrivateItemSuccess() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousItemAdminRequiredNotFound() throws Exception {
        configurationService.setProperty("versioning.item.history.view.admin", true);
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void epersonItemAdminRequiredNotFound() throws Exception {
        configurationService.setProperty("versioning.item.history.view.admin", true);
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminItemAdminRequiredSuccess() throws Exception {
        configurationService.setProperty("versioning.item.history.view.admin", true);
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousPrivateItemAdminRequiredNotFound() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        configurationService.setProperty("versioning.item.history.view.admin", true);
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void epersonPrivateItemAdminRequiredNotFound() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        configurationService.setProperty("versioning.item.history.view.admin", true);
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void adminPrivateItemAdminRequiredSuccess() throws Exception {
        authorizeService.removeAllPolicies(context, itemA);
        configurationService.setProperty("versioning.item.history.view.admin", true);
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(itemARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminBitstreamNotFound() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
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
