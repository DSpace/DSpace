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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.CommunityConverter;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageSubmitterGroup authorization feature
 */
public class ManageSubmitterGroupFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private CommunityConverter communityConverter;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    private Community communityA;
    private CommunityRest communityARest;
    private Collection collectionA;
    private CollectionRest collectionARest;

    final String feature = "canManageSubmitterGroup";

    // Static UUIDs for reloading shared fixtures across test instances
    private static UUID communityAId;
    private static UUID collectionAId;
    private static boolean sharedFixturesCreated = false;

    // Auth token cache: shared fixtures persist across tests so JWT tokens remain valid
    private static final Map<String, String> authTokenCache = new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        if (!sharedFixturesCreated) {
            context.turnOffAuthorisationSystem();

            communityA = CommunityBuilder.createCommunity(context)
                .withName("communityA")
                .build();
            collectionA = CollectionBuilder.createCollection(context, communityA)
                .withName("collectionA")
                .build();

            context.restoreAuthSystemState();

            // Store UUIDs for reloading into subsequent test contexts
            communityAId = communityA.getID();
            collectionAId = collectionA.getID();

            // Commit shared fixtures to the database
            context.commit();

            // Deregister shared fixtures from builder auto-cleanup so that
            // @AfterEach destroy() does not delete them after this test
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs into the current test's Context
            communityA = communityService.find(context, communityAId);
            collectionA = collectionService.find(context, collectionAId);
        }

        // Reload eperson into current session to avoid stale proxy
        // issues when prior test classes leave detached state
        eperson = context.reloadEntity(eperson);

        communityARest = communityConverter.convert(communityA, Projection.DEFAULT);
        collectionARest = collectionConverter.convert(collectionA, Projection.DEFAULT);
    }

    /**
     * Clean up shared fixtures after all tests have completed.
     * Deleting the community cascades to the collection.
     */
    @AfterAll
    public static void tearDownSharedFixtures() throws Exception {
        if (!sharedFixturesCreated) {
            return;
        }
        Context ctx = new Context(Context.Mode.READ_WRITE);
        ctx.turnOffAuthorisationSystem();

        CommunityService comService =
            ContentServiceFactory.getInstance().getCommunityService();

        Community com = comService.find(ctx, communityAId);
        if (com != null) {
            comService.delete(ctx, com);
        }

        ctx.complete();
        sharedFixturesCreated = false;
        authTokenCache.clear();
    }

    /**
     * Return a cached auth token for the given email, creating one if needed.
     *
     * @param email the email address to authenticate
     * @return the JWT token
     * @throws Exception if authentication fails
     */
    private String getCachedAuthToken(String email) throws Exception {
        String token = authTokenCache.get(email);
        if (token == null) {
            token = getAuthToken(email, password);
            authTokenCache.put(email, token);
        }
        return token;
    }

    @Test
    public void adminCollectionTestSuccess() throws Exception {
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
    public void adminCommunityTestNotFound() throws Exception {
        String adminToken = getCachedAuthToken(admin.getEmail());
        getClient(adminToken).perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void anonymousCollectionTestNotFound() throws Exception {
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
    public void anonymousCommunityTestNotFound() throws Exception {
        getClient().perform(
            get("/api/authz/authorizations/search/object")
                .param("embed", "feature")
                .param("feature", feature)
                .param("uri", utils.linkToSingleResource(communityARest, "self").getHref()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)))
            .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void collectionAdminCollectionTestSuccess() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .build();
        context.restoreAuthSystemState();

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
    public void collectionAdminCommunityTestNotFound() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .build();
        context.restoreAuthSystemState();

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
}
