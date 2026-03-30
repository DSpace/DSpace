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
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.converter.SiteConverter;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
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
import org.dspace.content.Site;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canViewWorkflowStatistics authorization feature.
 * <p>
 * Shared test fixtures are created once on the first test's
 * {@code @BeforeEach} and reused across all tests via static UUID fields.
 * </p>
 */
public class ViewWorkflowStatisticsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SiteConverter siteConverter;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    SiteService siteService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    private Site site;
    private SiteRest siteRest;
    private Community communityA;
    private CommunityRest communityARest;
    private Collection collectionA;
    private CollectionRest collectionARest;
    private Item itemA;
    private Bitstream bitstreamA;
    private BitstreamRest bitstreamARest;
    private Bundle bundleA;

    final String feature = "canViewWorkflowStatistics";

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

        site = siteService.findSite(context);

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

        // Reload site into current session (may be detached after
        // context.commit() in the first-test branch)
        site = siteService.findSite(context);

        siteRest = siteConverter.convert(site, Projection.DEFAULT);
        communityARest = communityConverter.convert(
            communityA, Projection.DEFAULT);
        collectionARest = collectionConverter.convert(
            collectionA, Projection.DEFAULT);
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
    public void adminBitstreamTestNotFound() throws Exception {
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

    @Test
    public void adminCollectionAdminRequiredSuccess() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
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
    public void adminCommunityAdminRequiredSuccess() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void adminSiteAdminRequiredSuccess() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonCollectionAdminRequiredNotFound() throws Exception {
        String epersonToken = getCachedAuthToken(eperson.getEmail());
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
    public void ePersonCommunityAdminRequiredNotFound() throws Exception {
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void ePersonSiteAdminRequiredNotFound() throws Exception {
        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void ePersonCollectionAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        String epersonToken = getCachedAuthToken(eperson.getEmail());
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
    public void ePersonCommunityAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void ePersonSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        String epersonToken = getCachedAuthToken(eperson.getEmail());
        getClient(epersonToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
            .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousCollectionAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(collectionARest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousCommunityAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }

    @Test
    public void anonymousSiteAdminNotRequiredSuccess() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.workflow", false);

        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(siteRest, "self").getHref()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                               .andExpect(jsonPath("$._embedded").exists());
    }
}
