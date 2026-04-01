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
import java.util.stream.Stream;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the canManageGroups authorization feature.
 * <p>
 * Tests verify authorization across 4 configuration scenarios (default, no-community-admin,
 * no-collection-admin, no-comcol-admin) and 15 actor types (5 direct users, 5 subgroup members,
 * 5 sub-subgroup members).
 * </p>
 * <p>
 * Shared test fixtures (EPersons, Communities, Collection) are created once on the first test's
 * {@code @BeforeEach} and reused across all tests via static UUID fields. Per-test state
 * (subgroups, config changes) is created and cleaned up normally per test.
 * </p>
 */
public class ManageGroupsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private IndexingService indexingService;

    private Community topLevelCommunity;
    private Community subCommunity;
    private Collection collection;

    private EPerson communityAdmin;
    private EPerson subCommunityAdmin;
    private EPerson collectionAdmin;
    private EPerson submitter;

    // Static UUIDs for reloading shared fixtures across test instances
    private static UUID communityAdminId;
    private static UUID subCommunityAdminId;
    private static UUID collectionAdminId;
    private static UUID submitterId;
    private static UUID topLevelCommunityId;
    private static UUID subCommunityId;
    private static UUID sharedCollectionId;
    private static boolean sharedFixturesCreated = false;

    // Auth token cache: shared fixtures persist across tests so JWT tokens remain valid
    private static final Map<String, String> authTokenCache = new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        if (!sharedFixturesCreated) {
            // Create shared fixtures once on the first test
            context.turnOffAuthorisationSystem();

            communityAdmin = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jhon", "Brown")
                .withEmail("communityAdmin@my.edu")
                .withPassword(password)
                .build();
            topLevelCommunity = CommunityBuilder.createCommunity(context)
                .withName("topLevelCommunity")
                .withAdminGroup(communityAdmin)
                .build();

            subCommunityAdmin = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jhon", "Brown")
                .withEmail("subCommunityAdmin@my.edu")
                .withPassword(password)
                .build();
            subCommunity = CommunityBuilder.createCommunity(context)
                .withName("subCommunity")
                .withAdminGroup(subCommunityAdmin)
                .addParentCommunity(context, topLevelCommunity)
                .build();

            submitter = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jhon", "Brown")
                .withEmail("submitter@my.edu")
                .withPassword(password)
                .build();
            collectionAdmin = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jhon", "Brown")
                .withEmail("collectionAdmin@my.edu")
                .withPassword(password)
                .build();
            collection = CollectionBuilder.createCollection(context, subCommunity)
                .withName("collection")
                .withAdminGroup(collectionAdmin)
                .withSubmitterGroup(submitter)
                .build();

            context.restoreAuthSystemState();

            // Store UUIDs for reloading into subsequent test contexts
            communityAdminId = communityAdmin.getID();
            subCommunityAdminId = subCommunityAdmin.getID();
            collectionAdminId = collectionAdmin.getID();
            submitterId = submitter.getID();
            topLevelCommunityId = topLevelCommunity.getID();
            subCommunityId = subCommunity.getID();
            sharedCollectionId = collection.getID();

            // Commit shared fixtures to the database
            context.commit();

            // Deregister shared fixtures from builder auto-cleanup so that
            // @AfterEach destroy() does not delete them after this test
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs into the current test's Context
            communityAdmin = ePersonService.find(context, communityAdminId);
            subCommunityAdmin = ePersonService.find(context, subCommunityAdminId);
            collectionAdmin = ePersonService.find(context, collectionAdminId);
            submitter = ePersonService.find(context, submitterId);
            topLevelCommunity = communityService.find(context, topLevelCommunityId);
            subCommunity = communityService.find(context, subCommunityId);
            collection = collectionService.find(context, sharedCollectionId);

            // Re-index shared fixtures in Solr (the mock Solr index is cleared
            // by @AfterEach destroy()). This is necessary because
            // isCommunityAdmin()/isCollectionAdmin() query Solr.
            indexingService.indexContent(
                context, new IndexableCommunity(topLevelCommunity), true, false);
            indexingService.indexContent(
                context, new IndexableCommunity(subCommunity), true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collection), true, true);
        }

        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff",
            "true");
    }

    /**
     * Clean up shared fixtures after all tests have completed.
     * Deletes in FK-safe order: collection, subcommunity, community, epersons.
     */
    @AfterAll
    public static void tearDownSharedFixtures() throws Exception {
        if (!sharedFixturesCreated) {
            return;
        }
        Context ctx = new Context(Context.Mode.READ_WRITE);
        ctx.turnOffAuthorisationSystem();

        CollectionService colService =
            ContentServiceFactory.getInstance().getCollectionService();
        CommunityService comService =
            ContentServiceFactory.getInstance().getCommunityService();
        EPersonService epService =
            EPersonServiceFactory.getInstance().getEPersonService();

        Collection col = colService.find(ctx, sharedCollectionId);
        if (col != null) {
            colService.delete(ctx, col);
        }
        Community sub = comService.find(ctx, subCommunityId);
        if (sub != null) {
            comService.delete(ctx, sub);
        }
        Community top = comService.find(ctx, topLevelCommunityId);
        if (top != null) {
            comService.delete(ctx, top);
        }
        for (UUID id : new UUID[]{communityAdminId, subCommunityAdminId,
            collectionAdminId, submitterId}) {
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

    /**
     * Apply the given configuration scenario.
     *
     * @param configName one of DEFAULT, NO_COMMUNITY, NO_COLLECTION, NO_COM_COL
     */
    private void applyConfig(String configName) {
        if ("NO_COMMUNITY".equals(configName) || "NO_COM_COL".equals(configName)) {
            configurationService.setProperty(
                "core.authorization.community-admin.policies", "false");
            configurationService.setProperty(
                "core.authorization.community-admin.admin-group", "false");
        }
        if ("NO_COLLECTION".equals(configName)
            || "NO_COM_COL".equals(configName)) {
            configurationService.setProperty(
                "core.authorization.community-admin.collection.policies",
                "false");
            configurationService.setProperty(
                "core.authorization.community-admin.collection.submitters",
                "false");
            configurationService.setProperty(
                "core.authorization.community-admin.collection.workflows",
                "false");
            configurationService.setProperty(
                "core.authorization.community-admin.collection.admin-group",
                "false");
        }
    }

    /**
     * Resolve a symbolic group key to the actual group name.
     *
     * @param key one of ADMIN, COMMUNITY_ADMIN, SUBCOMMUNITY_ADMIN,
     *            COLLECTION_ADMIN, COLLECTION_SUBMIT
     * @return the DSpace group name
     */
    private String resolveGroupName(String key) {
        return switch (key) {
            case "ADMIN" -> Group.ADMIN;
            case "COMMUNITY_ADMIN" ->
                "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN";
            case "SUBCOMMUNITY_ADMIN" ->
                "COMMUNITY_" + subCommunity.getID() + "_ADMIN";
            case "COLLECTION_ADMIN" ->
                "COLLECTION_" + collection.getID() + "_ADMIN";
            case "COLLECTION_SUBMIT" ->
                "COLLECTION_" + collection.getID() + "_SUBMIT";
            default ->
                throw new IllegalArgumentException("Unknown group key: " + key);
        };
    }

    /**
     * Assert whether the canManageGroups feature exists or not for the given
     * auth token.
     */
    private void assertFeature(String token, boolean expected)
        throws Exception {
        String siteId = siteService.findSite(context).getID().toString();
        String url = "/api/authz/authorizations/search/object"
            + "?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteId;

        if (expected) {
            getClient(token).perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                    "$._embedded.authorizations[?(@._embedded.feature"
                        + ".id=='canManageGroups')]")
                    .exists());
        } else {
            getClient(token).perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                    "$._embedded.authorizations[?(@._embedded.feature"
                        + ".id=='canManageGroups')]")
                    .doesNotExist());
        }
    }

    // -------------------------------------------------------------------------
    // Direct actor tests (5 assertions per config, 4 tests total)
    // -------------------------------------------------------------------------

    @Test
    public void testDirectActorsDefaultConfig() throws Exception {
        assertFeature(getCachedAuthToken(admin.getEmail()), true);
        assertFeature(getCachedAuthToken(communityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(subCommunityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(collectionAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(submitter.getEmail()), false);
    }

    @Test
    public void testDirectActorsNoCommunityGroupPermission() throws Exception {
        applyConfig("NO_COMMUNITY");
        assertFeature(getCachedAuthToken(admin.getEmail()), true);
        assertFeature(getCachedAuthToken(communityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(subCommunityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(collectionAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(submitter.getEmail()), false);
    }

    @Test
    public void testDirectActorsNoCollectionGroupPermission() throws Exception {
        applyConfig("NO_COLLECTION");
        assertFeature(getCachedAuthToken(admin.getEmail()), true);
        assertFeature(getCachedAuthToken(communityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(subCommunityAdmin.getEmail()), true);
        assertFeature(getCachedAuthToken(collectionAdmin.getEmail()), false);
        assertFeature(getCachedAuthToken(submitter.getEmail()), false);
    }

    @Test
    public void testDirectActorsNoComColGroupPermission() throws Exception {
        applyConfig("NO_COM_COL");
        assertFeature(getCachedAuthToken(admin.getEmail()), true);
        assertFeature(getCachedAuthToken(communityAdmin.getEmail()), false);
        assertFeature(getCachedAuthToken(subCommunityAdmin.getEmail()), false);
        assertFeature(getCachedAuthToken(collectionAdmin.getEmail()), false);
        assertFeature(getCachedAuthToken(submitter.getEmail()), false);
    }

    // -------------------------------------------------------------------------
    // Parameterized subgroup and sub-subgroup tests
    // -------------------------------------------------------------------------

    /**
     * Test data for subgroup and sub-subgroup authorization tests.
     * Each entry: (configName, parentGroupKey, expectedResult).
     *
     * @return stream of test arguments
     */
    static Stream<Arguments> groupTestCases() {
        return Stream.of(
            // Default config
            Arguments.of("DEFAULT", "ADMIN", true),
            Arguments.of("DEFAULT", "COMMUNITY_ADMIN", true),
            Arguments.of("DEFAULT", "SUBCOMMUNITY_ADMIN", true),
            Arguments.of("DEFAULT", "COLLECTION_ADMIN", true),
            Arguments.of("DEFAULT", "COLLECTION_SUBMIT", false),
            // No community group permission
            Arguments.of("NO_COMMUNITY", "ADMIN", true),
            Arguments.of("NO_COMMUNITY", "COMMUNITY_ADMIN", true),
            Arguments.of("NO_COMMUNITY", "SUBCOMMUNITY_ADMIN", true),
            Arguments.of("NO_COMMUNITY", "COLLECTION_ADMIN", true),
            Arguments.of("NO_COMMUNITY", "COLLECTION_SUBMIT", false),
            // No collection group permission
            Arguments.of("NO_COLLECTION", "ADMIN", true),
            Arguments.of("NO_COLLECTION", "COMMUNITY_ADMIN", true),
            Arguments.of("NO_COLLECTION", "SUBCOMMUNITY_ADMIN", true),
            Arguments.of("NO_COLLECTION", "COLLECTION_ADMIN", false),
            Arguments.of("NO_COLLECTION", "COLLECTION_SUBMIT", false),
            // No community or collection group permission
            Arguments.of("NO_COM_COL", "ADMIN", true),
            Arguments.of("NO_COM_COL", "COMMUNITY_ADMIN", false),
            Arguments.of("NO_COM_COL", "SUBCOMMUNITY_ADMIN", false),
            Arguments.of("NO_COM_COL", "COLLECTION_ADMIN", false),
            Arguments.of("NO_COM_COL", "COLLECTION_SUBMIT", false)
        );
    }

    @ParameterizedTest(name = "subgroup of {1}, config={0}, expected={2}")
    @MethodSource("groupTestCases")
    public void testSubGroupPermission(String configName,
                                       String parentGroupKey,
                                       boolean expected) throws Exception {
        applyConfig(configName);

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(
                context, resolveGroupName(parentGroupKey)))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        assertFeature(getCachedAuthToken(eperson.getEmail()), expected);
    }

    @ParameterizedTest(name = "sub-subgroup of {1}, config={0}, expected={2}")
    @MethodSource("groupTestCases")
    public void testSubSubGroupPermission(String configName,
                                          String parentGroupKey,
                                          boolean expected) throws Exception {
        applyConfig(configName);

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(
                context, resolveGroupName(parentGroupKey)))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        assertFeature(getCachedAuthToken(eperson.getEmail()), expected);
    }
}
