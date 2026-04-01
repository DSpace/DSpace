/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import static org.hamcrest.Matchers.contains;
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
import org.dspace.app.rest.authorization.impl.DownloadFeature;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the download authorization feature.
 * <p>
 * Shared test fixtures are created once on the first test's
 * {@code @BeforeEach} and reused across all tests via static UUID fields.
 * </p>
 */
public class DownloadFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private Utils utils;

    private AuthorizationFeature downloadFeature;

    private Collection collectionA;
    private Item itemA;
    private Bitstream bitstreamA;
    private Bitstream bitstreamB;

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

        downloadFeature = authorizationFeatureService
            .find(DownloadFeature.NAME);

        if (!sharedFixturesCreated) {
            context.turnOffAuthorisationSystem();

            Community communityA = CommunityBuilder
                .createCommunity(context).build();
            collectionA = CollectionBuilder
                .createCollection(context, communityA)
                .withLogo("Blub").build();

            itemA = ItemBuilder.createItem(context, collectionA)
                .build();

            context.restoreAuthSystemState();

            // Store UUIDs for reloading
            communityAId = communityA.getID();
            collectionAId = collectionA.getID();
            itemAId = itemA.getID();

            context.commit();
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs
            collectionA = collectionService.find(context,
                collectionAId);
            itemA = itemService.find(context, itemAId);
        }

        // Reload eperson into current session
        eperson = context.reloadEntity(eperson);

        // Create bitstreams per test (cleaned up by
        // AbstractBuilder after each test)
        context.turnOffAuthorisationSystem();
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(
                bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamA = BitstreamBuilder
                .createBitstream(context, itemA, is)
                .withName("Bitstream")
                .withDescription("Description")
                .withMimeType("text/plain")
                .build();
            bitstreamB = BitstreamBuilder
                .createBitstream(context, itemA, is)
                .withName("Bitstream2")
                .withDescription("Description2")
                .withMimeType("text/plain")
                .build();
        }
        resourcePolicyService.removePolicies(
            context, bitstreamB, Constants.READ);
        context.restoreAuthSystemState();
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
    public void downloadOfCollectionAAsAdmin() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getCachedAuthToken(admin.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsAdmin() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getCachedAuthToken(admin.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void downloadOfBitstreamAAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(admin, downloadFeature, bitstreamRest);

        String token = getCachedAuthToken(admin.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsAdmin() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(admin, downloadFeature, bitstreamRest);

        String token = getCachedAuthToken(admin.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }


    // Tests for anonymous user
    @Test
    public void downloadOfCollectionAAsAnonymous() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", collectionUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsAnonymous() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", itemUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    @Test
    public void downloadOfBitstreamAAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(null, downloadFeature, bitstreamRest);

        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                   .andExpect(jsonPath("$._embedded.authorizations", contains(
                           Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsAnonymous() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();


        getClient().perform(get("/api/authz/authorizations/search/object")
                                    .param("uri", bitstreamUri)
                                    .param("feature", downloadFeature.getName()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    // Test for Eperson
    @Test
    public void downloadOfCollectionAAsEperson() throws Exception {
        CollectionRest collectionRest = collectionConverter.convert(collectionA, Projection.DEFAULT);
        String collectionUri = utils.linkToSingleResource(collectionRest, "self").getHref();

        String token = getCachedAuthToken(eperson.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", collectionUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfItemAAsEperson() throws Exception {
        ItemRest itemRest = itemConverter.convert(itemA, Projection.DEFAULT);
        String itemUri = utils.linkToSingleResource(itemRest, "self").getHref();

        String token = getCachedAuthToken(eperson.getEmail());


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", itemUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void downloadOfBitstreamAAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamA, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        Authorization authorizationFeature = new Authorization(eperson, downloadFeature, bitstreamRest);

        String token = getCachedAuthToken(eperson.getEmail());


        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", greaterThan(0)))
                        .andExpect(jsonPath("$._embedded.authorizations", contains(
                                Matchers.is(AuthorizationMatcher.matchAuthorization(authorizationFeature)))));

    }

    @Test
    public void downloadOfBitstreamBAsEperson() throws Exception {
        BitstreamRest bitstreamRest = bitstreamConverter.convert(bitstreamB, Projection.DEFAULT);
        String bitstreamUri = utils.linkToSingleResource(bitstreamRest, "self").getHref();

        String token = getCachedAuthToken(eperson.getEmail());

        getClient(token).perform(get("/api/authz/authorizations/search/object")
                                         .param("uri", bitstreamUri)
                                         .param("feature", downloadFeature.getName()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

}
