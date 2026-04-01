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
import org.dspace.app.rest.authorization.impl.CanManageMappingsFeature;
import org.dspace.app.rest.converter.BitstreamConverter;
import org.dspace.app.rest.converter.CollectionConverter;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageMappings authorization feature.
 * <p>
 * Shared test fixtures are created once on the first test's
 * {@code @BeforeEach} and reused across all tests via static UUID fields.
 * </p>
 */
public class CanManageMappingsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private CollectionConverter collectionConverter;

    @Autowired
    private BitstreamConverter bitstreamConverter;

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private IndexingService indexingService;

    private EPerson userA;
    private Community communityA;
    private Collection collectionA;
    private Collection collectionB;
    private CollectionRest collectionARest;
    private Item itemA;
    private Bitstream bitstreamA;
    private BitstreamRest bitstreamARest;
    private Bundle bundleA;
    private AuthorizationFeature canManageMappingsFeature;

    final String feature = "canManageMappings";

    // Static UUIDs for reloading shared fixtures
    private static UUID userAId;
    private static UUID communityAId;
    private static UUID collectionAId;
    private static UUID collectionBId;
    private static UUID itemAId;
    private static boolean sharedFixturesCreated = false;

    private static final Map<String, String> authTokenCache =
        new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        canManageMappingsFeature = authorizationFeatureService
            .find(CanManageMappingsFeature.NAME);

        if (!sharedFixturesCreated) {
            context.turnOffAuthorisationSystem();

            userA = EPersonBuilder.createEPerson(context)
                .withEmail("userEmail@test.com")
                .withPassword(password).build();

            communityA = CommunityBuilder.createCommunity(context)
                .withName("communityA")
                .build();
            collectionA = CollectionBuilder
                .createCollection(context, communityA)
                .withName("collectionA")
                .build();
            collectionB = CollectionBuilder
                .createCollection(context, communityA)
                .withName("collectionB")
                .build();
            itemA = ItemBuilder.createItem(context, collectionA)
                .withTitle("itemA")
                .build();

            context.restoreAuthSystemState();

            // Store UUIDs for reloading
            userAId = userA.getID();
            communityAId = communityA.getID();
            collectionAId = collectionA.getID();
            collectionBId = collectionB.getID();
            itemAId = itemA.getID();

            context.commit();
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs
            userA = ePersonService.find(context, userAId);
            communityA = communityService.find(context,
                communityAId);
            collectionA = collectionService.find(context,
                collectionAId);
            collectionB = collectionService.find(context,
                collectionBId);
            itemA = itemService.find(context, itemAId);

            // Re-index shared fixtures in Solr (cleared by
            // @AfterEach destroy())
            indexingService.indexContent(context,
                new IndexableCommunity(communityA),
                true, false);
            indexingService.indexContent(context,
                new IndexableCollection(collectionA),
                true, false);
            indexingService.indexContent(context,
                new IndexableCollection(collectionB),
                true, false);
            indexingService.indexContent(context,
                new IndexableItem(itemA),
                true, true);
        }

        // Reload eperson into current session
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

        // Convert to REST representations (needs session-attached
        // entities)
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

        org.dspace.content.service.CommunityService comService =
            ContentServiceFactory.getInstance()
                .getCommunityService();
        EPersonService epService =
            EPersonServiceFactory.getInstance().getEPersonService();

        Community com = comService.find(ctx, communityAId);
        if (com != null) {
            comService.delete(ctx, com);
        }
        EPerson ep = epService.find(ctx, userAId);
        if (ep != null) {
            epService.delete(ctx, ep);
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
    public void adminCollectionAdminSuccess() throws Exception {
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
    public void epersonCollectionNotFound() throws Exception {
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
    public void addWriteEpersonCollectionSuccess() throws Exception {
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADD)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(collectionA)
            .withAction(Constants.WRITE)
            .build();

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
    public void adminEpersonCollectionSuccess() throws Exception {
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withDspaceObject(collectionA)
            .withAction(Constants.ADMIN)
            .build();

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
    @SuppressWarnings("unchecked")
    public void canManageMappingsWithUserThatCanManageTwoCollectionsTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(collectionA)
                             .withAction(Constants.ADD).build();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(collectionB)
                             .withAction(Constants.ADD).build();
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenEPerson = getCachedAuthToken(eperson.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageMappingsFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageMappingsFeature, itemRestA);

        // define authorization that we know not exists
        Authorization eperson2ItemA = new Authorization(eperson, canManageMappingsFeature, itemRestA);
        Authorization anonymous2ItemA = new Authorization(null, canManageMappingsFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemA.getID()))
                   .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void canManageMappingsOnlyAdminHasAccessTest() throws Exception {
        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageMappingsFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userA2ItemA = new Authorization(userA, canManageMappingsFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void canManageMappingsCommunityAdminAndCollectionsAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson userComAdmin = EPersonBuilder.createEPerson(context)
                                      .withEmail("userComAdminEmail@test.com")
                                      .withPassword(password).build();

        EPerson user1 = EPersonBuilder.createEPerson(context)
                                      .withEmail("user1Email@test.com")
                                      .withPassword(password).build();

        EPerson user2 = EPersonBuilder.createEPerson(context)
                                      .withEmail("user2Email@test.com")
                                      .withPassword(password).build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Community subCommunity1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity 1")
                                                  .withAdminGroup(userComAdmin)
                                                  .build();

        Community subCommunity2 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                  .withName("SubCommunity 2")
                                                  .build();

        Collection col1 = CollectionBuilder.createCollection(context, subCommunity1)
                                           .withName("Collection 1")
                                           .withAdminGroup(user1).build();

        Collection col2 = CollectionBuilder.createCollection(context, subCommunity1)
                                           .withName("Collection 2")
                                           .withAdminGroup(user1, user2).build();

        Collection col3 = CollectionBuilder.createCollection(context, subCommunity2)
                                           .withName("Collection 3").build();

        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Test Item 1")
                                .build();

        Item item2 = ItemBuilder.createItem(context, col2)
                                .withTitle("Test Item 2")
                                .build();

        Item item3 = ItemBuilder.createItem(context, col3)
                                .withTitle("Test Item 3")
                                .build();

        context.restoreAuthSystemState();

        ItemRest itemRest1 = itemConverter.convert(item1, DefaultProjection.DEFAULT);
        ItemRest itemRest2 = itemConverter.convert(item2, DefaultProjection.DEFAULT);
        ItemRest itemRest3 = itemConverter.convert(item3, DefaultProjection.DEFAULT);
        CollectionRest colRest1 = collectionConverter.convert(col1, DefaultProjection.DEFAULT);
        CollectionRest colRest2 = collectionConverter.convert(col2, DefaultProjection.DEFAULT);
        CollectionRest colRest3 = collectionConverter.convert(col3, DefaultProjection.DEFAULT);

        String tokenUserComAdmin = getAuthToken(userComAdmin.getEmail(), password);
        String tokenUser1 = getAuthToken(user1.getEmail(), password);
        String tokenUser2 = getAuthToken(user2.getEmail(), password);

        // define authorizations
        Authorization userComAdminItem1 = new Authorization(userComAdmin, canManageMappingsFeature, itemRest1);
        Authorization userComAdminItem2 = new Authorization(userComAdmin, canManageMappingsFeature, itemRest2);
        Authorization userComAdminItem3 = new Authorization(userComAdmin, canManageMappingsFeature, itemRest3);
        Authorization userComAdminCol1 = new Authorization(userComAdmin, canManageMappingsFeature, colRest1);
        Authorization userComAdminCol2 = new Authorization(userComAdmin, canManageMappingsFeature, colRest2);
        Authorization userComAdminCol3 = new Authorization(userComAdmin, canManageMappingsFeature, colRest3);

        Authorization user1Item1 = new Authorization(user1, canManageMappingsFeature, itemRest1);
        Authorization user1Item2 = new Authorization(user1, canManageMappingsFeature, itemRest2);
        Authorization user1Item3 = new Authorization(user1, canManageMappingsFeature, itemRest3);
        Authorization user1Col1 = new Authorization(user1, canManageMappingsFeature, colRest1);
        Authorization user1Col2 = new Authorization(user1, canManageMappingsFeature, colRest2);
        Authorization user1Col3 = new Authorization(user1, canManageMappingsFeature, colRest3);

        Authorization user2Item1 = new Authorization(user2, canManageMappingsFeature, itemRest1);
        Authorization user2Item2 = new Authorization(user2, canManageMappingsFeature, itemRest2);
        Authorization user2Item3 = new Authorization(user2, canManageMappingsFeature, itemRest3);
        Authorization user2Col1 = new Authorization(user2, canManageMappingsFeature, colRest1);
        Authorization user2Col2 = new Authorization(user2, canManageMappingsFeature, colRest2);
        Authorization user2Col3 = new Authorization(user2, canManageMappingsFeature, colRest3);

        //Community admin
        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminItem1.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userComAdminItem1))));

        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminItem2.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userComAdminItem2))));

        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminCol1.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userComAdminCol1))));

        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminCol2.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userComAdminCol2))));

        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminItem3.getID()))
                                    .andExpect(status().isNotFound());

        getClient(tokenUserComAdmin).perform(get("/api/authz/authorizations/" + userComAdminCol3.getID()))
                                    .andExpect(status().isNotFound());

        // user 1
        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Item1.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user1Item1))));

        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Item2.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user1Item2))));

        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Col1.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user1Col1))));

        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Col2.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user1Col2))));

        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Item3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenUser1).perform(get("/api/authz/authorizations/" + user1Col3.getID()))
                             .andExpect(status().isNotFound());

        // user 2
        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Col2.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(user2Col2))));

        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Item1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Item2.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Col1.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Item3.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenUser2).perform(get("/api/authz/authorizations/" + user2Col3.getID()))
                             .andExpect(status().isNotFound());
    }

}
