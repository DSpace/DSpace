/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.authorization.impl.CanManageBitstreamBundlesFeature;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
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
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for the canManageBitstreamBundles authorization feature.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class CanManageBitstreamBundlesFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ItemConverter itemConverter;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ConfigurationService configurationService;

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

    private Item itemA;
    private Item itemB;
    private EPerson userA;
    private EPerson userB;
    private EPerson userColAadmin;
    private EPerson userColBadmin;
    private EPerson userComAdmin;
    private Community communityA;
    private Collection collectionA;
    private Collection collectionB;
    private AuthorizationFeature canManageBitstreamBundlesFeature;

    final String feature = "canManageBitstreamBundles";

    // Static UUIDs for reloading shared fixtures across test instances
    private static UUID userAId;
    private static UUID userBId;
    private static UUID userColAadminId;
    private static UUID userColBadminId;
    private static UUID userComAdminId;
    private static UUID communityAId;
    private static UUID collectionAId;
    private static UUID collectionBId;
    private static UUID itemAId;
    private static UUID itemBId;
    private static boolean sharedFixturesCreated = false;

    // Auth token cache: shared fixtures persist across tests so JWT tokens remain valid
    private static final Map<String, String> authTokenCache = new HashMap<>();

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        canManageBitstreamBundlesFeature = authorizationFeatureService
            .find(CanManageBitstreamBundlesFeature.NAME);

        if (!sharedFixturesCreated) {
            // Create shared fixtures once on the first test
            context.turnOffAuthorisationSystem();

            userA = EPersonBuilder.createEPerson(context)
                                 .withEmail("userEmail@test.com")
                                 .withPassword(password).build();

            userB = EPersonBuilder.createEPerson(context)
                                  .withEmail("userB.email@test.com")
                                  .withPassword(password).build();

            userColAadmin = EPersonBuilder.createEPerson(context)
                                         .withEmail("userColAadmin@test.com")
                                         .withPassword(password).build();

            userColBadmin = EPersonBuilder.createEPerson(context)
                                          .withEmail("userColBadmin@test.com")
                                          .withPassword(password).build();

            userComAdmin = EPersonBuilder.createEPerson(context)
                                         .withEmail("userComAdmin@test.com")
                                         .withPassword(password).build();

            communityA = CommunityBuilder.createCommunity(context)
                                         .withName("communityA")
                                         .withAdminGroup(userComAdmin).build();

            collectionA = CollectionBuilder
                .createCollection(context, communityA)
                .withName("Collection A")
                .withAdminGroup(userColAadmin).build();

            collectionB = CollectionBuilder
                .createCollection(context, communityA)
                .withName("Collection B")
                .withAdminGroup(userColBadmin).build();

            itemA = ItemBuilder.createItem(context, collectionA)
                               .withTitle("Item A").build();

            itemB = ItemBuilder.createItem(context, collectionB)
                               .withTitle("Item B").build();

            context.restoreAuthSystemState();

            // Store UUIDs for reloading into subsequent test contexts
            userAId = userA.getID();
            userBId = userB.getID();
            userColAadminId = userColAadmin.getID();
            userColBadminId = userColBadmin.getID();
            userComAdminId = userComAdmin.getID();
            communityAId = communityA.getID();
            collectionAId = collectionA.getID();
            collectionBId = collectionB.getID();
            itemAId = itemA.getID();
            itemBId = itemB.getID();

            // Commit shared fixtures to the database
            context.commit();

            // Deregister shared fixtures from builder auto-cleanup so that
            // @AfterEach destroy() does not delete them after this test
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs into the current test's Context
            userA = ePersonService.find(context, userAId);
            userB = ePersonService.find(context, userBId);
            userColAadmin = ePersonService.find(context, userColAadminId);
            userColBadmin = ePersonService.find(context, userColBadminId);
            userComAdmin = ePersonService.find(context, userComAdminId);
            communityA = communityService.find(context, communityAId);
            collectionA = collectionService.find(context, collectionAId);
            collectionB = collectionService.find(context, collectionBId);
            itemA = itemService.find(context, itemAId);
            itemB = itemService.find(context, itemBId);

            // Re-index shared fixtures in Solr (the mock Solr index is
            // cleared by @AfterEach destroy())
            indexingService.indexContent(
                context, new IndexableCommunity(communityA),
                true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collectionA),
                true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collectionB),
                true, false);
            indexingService.indexContent(
                context, new IndexableItem(itemA),
                true, false);
            indexingService.indexContent(
                context, new IndexableItem(itemB),
                true, true);
        }

        // Reload eperson into current session to avoid stale proxy
        // issues when prior test classes leave detached state
        eperson = context.reloadEntity(eperson);
    }

    /**
     * Clean up shared fixtures after all tests have completed.
     * Deletes communities (cascades to collections and items),
     * then epersons.
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
        EPersonService epService =
            EPersonServiceFactory.getInstance().getEPersonService();

        Community com = comService.find(ctx, communityAId);
        if (com != null) {
            comService.delete(ctx, com);
        }
        for (UUID id : new UUID[]{userAId, userBId, userColAadminId,
            userColBadminId, userComAdminId}) {
            EPerson ep = epService.find(ctx, id);
            if (ep != null) {
                epService.delete(ctx, ep);
            }
        }

        ctx.complete();
        sharedFixturesCreated = false;
        authTokenCache.clear();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private String getCachedAuthToken(String email) throws Exception {
        String token = authTokenCache.get(email);
        if (token == null) {
            token = getAuthToken(email, password);
            authTokenCache.put(email, token);
        }
        return token;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkCanCreateVersionsFeatureTest() throws Exception {
        context.turnOffAuthorisationSystem();
        //permissions for userA
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.ADD).build();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.REMOVE).build();
        // permissions for userB
        ResourcePolicyBuilder.createResourcePolicy(context, userB, null)
                             .withDspaceObject(itemA)
                             .withAction(Constants.REMOVE).build();
        ResourcePolicyBuilder.createResourcePolicy(context, userB, null)
                             .withDspaceObject(itemB)
                             .withAction(Constants.REMOVE).build();
        ResourcePolicyBuilder.createResourcePolicy(context, userB, null)
                             .withDspaceObject(itemB)
                             .withAction(Constants.ADD).build();

        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenEPerson = getCachedAuthToken(eperson.getEmail());
        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenBUser = getCachedAuthToken(userB.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());
        String tokenColBadmin = getCachedAuthToken(userColBadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userB2ItemB = new Authorization(userB, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userB2ItemA = new Authorization(userB, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemB = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestB);
        Authorization eperson2ItemA = new Authorization(eperson, canManageBitstreamBundlesFeature, itemRestA);
        Authorization eperson2ItemB = new Authorization(eperson, canManageBitstreamBundlesFeature, itemRestB);
        Authorization anonymous2ItemA = new Authorization(null, canManageBitstreamBundlesFeature, itemRestA);
        Authorization anonymous2ItemB = new Authorization(null, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemB = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colBadmin2ItemA = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userA2ItemA))));

        getClient(tokenBUser).perform(get("/api/authz/authorizations/" + userB2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(userB2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenBUser).perform(get("/api/authz/authorizations/" + userB2ItemA.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemB.getID()))
                             .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemA.getID()))
                               .andExpect(status().isNotFound());

        getClient(tokenEPerson).perform(get("/api/authz/authorizations/" + eperson2ItemB.getID()))
                               .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemA.getID()))
                   .andExpect(status().isNotFound());

        getClient().perform(get("/api/authz/authorizations/" + anonymous2ItemB.getID()))
                   .andExpect(status().isNotFound());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);

        // define authorization that we know not exists
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(colAadmin2ItemA))));

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCollectionAdminCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());
        String tokenColBadmin = getCachedAuthToken(userColBadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCollectionAdminDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());
        String tokenColBadmin = getCachedAuthToken(userColBadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemA))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(comAdmin2ItemB))));

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCommunityAdminCreateBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.community-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());
        String tokenColBadmin = getCachedAuthToken(userColBadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void itemAdminSetPropertyCommunityAdminDeleteBitstreamToFalseTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context, userA, null)
                             .withAction(Constants.ADMIN)
                             .withDspaceObject(itemA).build();

        configurationService.setProperty("core.authorization.community-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.create-bitstream", false);
        configurationService.setProperty("core.authorization.collection-admin.item.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.delete-bitstream", false);
        configurationService.setProperty("core.authorization.item-admin.create-bitstream", false);
        context.restoreAuthSystemState();

        ItemRest itemRestA = itemConverter.convert(itemA, DefaultProjection.DEFAULT);
        ItemRest itemRestB = itemConverter.convert(itemB, DefaultProjection.DEFAULT);

        String tokenAdmin = getCachedAuthToken(admin.getEmail());
        String tokenAUser = getCachedAuthToken(userA.getEmail());
        String tokenComAdmin = getCachedAuthToken(userComAdmin.getEmail());
        String tokenColAadmin = getCachedAuthToken(userColAadmin.getEmail());
        String tokenColBadmin = getCachedAuthToken(userColBadmin.getEmail());

        // define authorizations that we know must exists
        Authorization admin2ItemA = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization admin2ItemB = new Authorization(admin, canManageBitstreamBundlesFeature, itemRestB);

        // define authorization that we know not exists
        Authorization comAdmin2ItemA = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization comAdmin2ItemB = new Authorization(userComAdmin, canManageBitstreamBundlesFeature, itemRestB);
        Authorization colAadmin2ItemA = new Authorization(userColAadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization colBadmin2ItemB = new Authorization(userColBadmin, canManageBitstreamBundlesFeature, itemRestA);
        Authorization userA2ItemA = new Authorization(userA, canManageBitstreamBundlesFeature, itemRestA);

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemA.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemA))));

        getClient(tokenAdmin).perform(get("/api/authz/authorizations/" + admin2ItemB.getID()))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$", Matchers.is(AuthorizationMatcher.matchAuthorization(admin2ItemB))));

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemA.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenComAdmin).perform(get("/api/authz/authorizations/" + comAdmin2ItemB.getID()))
                                .andExpect(status().isNotFound());

        getClient(tokenColAadmin).perform(get("/api/authz/authorizations/" + colAadmin2ItemA.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenColBadmin).perform(get("/api/authz/authorizations/" + colBadmin2ItemB.getID()))
                                 .andExpect(status().isNotFound());

        getClient(tokenAUser).perform(get("/api/authz/authorizations/" + userA2ItemA.getID()))
                             .andExpect(status().isNotFound());

    }

}
