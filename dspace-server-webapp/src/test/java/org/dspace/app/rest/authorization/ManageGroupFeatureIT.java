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

import org.dspace.app.rest.authorization.impl.ManageGroupFeature;
import org.dspace.app.rest.converter.GroupConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.AbstractBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the canManageGroup authorization feature.
 * <p>
 * Tests verify whether specific actors (site admin, community admin,
 * collection admin, collection submitter) can manage various groups
 * (community admin groups, collection admin groups, collection submitter
 * groups, anonymous group, administrator group).
 * </p>
 * <p>
 * Shared test fixtures (EPersons, Communities, Collections, Groups) are
 * created once on the first test's {@code @BeforeEach} and reused across
 * all tests via static UUID fields.
 * </p>
 *
 * <pre>
 * Structure:
 * |_ community 1
 *    |_ (g) community1AdminGroup
 *       |_ (e) community1Admin
 *    |_ collection1
 *       |_ (g) collection1AdminGroup
 *          |_ (e) collection1Admin
 *       |_ (g) collection1SubmitterGroup
 *          |_ (e) collection1Submitter
 * |_ community2
 *    |_ (g) community2AdminGroup
 *       |_ (e) eperson
 *    |_ collection2
 *       |_ (g) collection2AdminGroup
 *          |_ (e) eperson
 *       |_ (g) collection2SubmitterGroup
 *          |_ (e) eperson
 * |_ community3
 *    |_ (no groups)
 *    |_ collection3
 *       |_ (no groups)
 * </pre>
 */
public class ManageGroupFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private GroupConverter groupConverter;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private IndexingService indexingService;

    private Community community1;
    private Group community1AdminGroup;
    private EPerson community1Admin;
    private Collection collection1;
    private Group collection1AdminGroup;
    private EPerson collection1Admin;
    private Group collection1SubmitterGroup;
    private EPerson collection1Submitter;

    private Community community2;
    private Group community2AdminGroup;
    private Collection collection2;
    private Group collection2AdminGroup;
    private Group collection2SubmitterGroup;

    private Community community3;
    private Collection collection3;

    private Group anonymousGroup;
    private Group administratorGroup;

    private AuthorizationFeature canManageGroupFeature;

    // Static UUIDs for reloading shared fixtures across test instances
    private static UUID community1Id;
    private static UUID community1AdminId;
    private static UUID collection1Id;
    private static UUID collection1AdminId;
    private static UUID collection1SubmitterId;
    private static UUID community2Id;
    private static UUID collection2Id;
    private static UUID community3Id;
    private static UUID collection3Id;
    private static boolean sharedFixturesCreated = false;

    // Auth token cache: shared fixtures persist so JWT tokens remain valid
    private static final Map<String, String> authTokenCache =
        new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        canManageGroupFeature = authorizationFeatureService
            .find(ManageGroupFeature.NAME);

        if (!sharedFixturesCreated) {
            context.turnOffAuthorisationSystem();

            community1Admin = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withEmail("community1admin@email.com")
                .withPassword(password)
                .withNameInMetadata("Admin", "Community 1")
                .build();

            community1 = CommunityBuilder.createCommunity(context)
                .withName("Community 1")
                .withAdminGroup(community1Admin)
                .build();

            community1AdminGroup = community1.getAdministrators();

            collection1Admin = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withEmail("collection1admin@email.com")
                .withPassword(password)
                .withNameInMetadata("Admin", "Collection 1")
                .build();

            collection1Submitter = EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withEmail("collection1submitter@email.com")
                .withPassword(password)
                .withNameInMetadata("Submitter", "Collection 1")
                .build();

            collection1 = CollectionBuilder
                .createCollection(context, community1)
                .withName("Collection 1")
                .withAdminGroup(collection1Admin)
                .withSubmitterGroup(collection1Submitter)
                .build();

            collection1AdminGroup = collection1.getAdministrators();
            collection1SubmitterGroup = collection1.getSubmitters();

            community2 = CommunityBuilder.createCommunity(context)
                .withName("Community 2")
                .withAdminGroup(eperson)
                .build();

            community2AdminGroup = community2.getAdministrators();

            collection2 = CollectionBuilder
                .createCollection(context, community2)
                .withName("Collection 2")
                .withAdminGroup(eperson)
                .withSubmitterGroup(eperson)
                .build();

            collection2AdminGroup = collection2.getAdministrators();
            collection2SubmitterGroup = collection2.getSubmitters();

            community3 = CommunityBuilder.createCommunity(context)
                .withName("Community 3")
                .build();

            collection3 = CollectionBuilder
                .createCollection(context, community3)
                .withName("Collection 3")
                .build();

            anonymousGroup = groupService
                .findByName(context, Group.ANONYMOUS);
            administratorGroup = groupService
                .findByName(context, Group.ADMIN);

            context.restoreAuthSystemState();

            // Store UUIDs for reloading into subsequent test contexts
            community1Id = community1.getID();
            community1AdminId = community1Admin.getID();
            collection1Id = collection1.getID();
            collection1AdminId = collection1Admin.getID();
            collection1SubmitterId = collection1Submitter.getID();
            community2Id = community2.getID();
            collection2Id = collection2.getID();
            community3Id = community3.getID();
            collection3Id = collection3.getID();

            // Commit shared fixtures to the database
            context.commit();

            // Deregister shared fixtures from builder auto-cleanup so that
            // @AfterEach destroy() does not delete them after this test
            AbstractBuilder.cleanupBuilderCache();
            sharedFixturesCreated = true;
        } else {
            // Reload shared fixtures from UUIDs into current test's Context
            community1Admin = ePersonService
                .find(context, community1AdminId);
            collection1Admin = ePersonService
                .find(context, collection1AdminId);
            collection1Submitter = ePersonService
                .find(context, collection1SubmitterId);

            community1 = communityService.find(context, community1Id);
            community1AdminGroup = community1.getAdministrators();

            collection1 = collectionService
                .find(context, collection1Id);
            collection1AdminGroup = collection1.getAdministrators();
            collection1SubmitterGroup = collection1.getSubmitters();

            community2 = communityService.find(context, community2Id);
            community2AdminGroup = community2.getAdministrators();

            collection2 = collectionService
                .find(context, collection2Id);
            collection2AdminGroup = collection2.getAdministrators();
            collection2SubmitterGroup = collection2.getSubmitters();

            community3 = communityService.find(context, community3Id);
            collection3 = collectionService
                .find(context, collection3Id);

            anonymousGroup = groupService
                .findByName(context, Group.ANONYMOUS);
            administratorGroup = groupService
                .findByName(context, Group.ADMIN);

            // Re-index shared fixtures in Solr (cleared by @AfterEach)
            indexingService.indexContent(
                context, new IndexableCommunity(community1),
                true, false);
            indexingService.indexContent(
                context, new IndexableCommunity(community2),
                true, false);
            indexingService.indexContent(
                context, new IndexableCommunity(community3),
                true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collection1),
                true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collection2),
                true, false);
            indexingService.indexContent(
                context, new IndexableCollection(collection3),
                true, true);
        }
    }

    /**
     * Clean up shared fixtures after all tests have completed.
     * Deletes in FK-safe order: collections, communities, epersons.
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

        for (UUID id : new UUID[]{collection1Id, collection2Id,
            collection3Id}) {
            Collection col = colService.find(ctx, id);
            if (col != null) {
                colService.delete(ctx, col);
            }
        }
        for (UUID id : new UUID[]{community1Id, community2Id,
            community3Id}) {
            Community com = comService.find(ctx, id);
            if (com != null) {
                comService.delete(ctx, com);
            }
        }
        for (UUID id : new UUID[]{community1AdminId,
            collection1AdminId, collection1SubmitterId}) {
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
     * Get the REST representation of the given group.
     *
     * @param group the group.
     * @return the REST representation of the group.
     */
    private GroupRest getGroupRest(Group group) throws Exception {
        return groupConverter.convert(
            context.reloadEntity(group), Projection.DEFAULT);
    }

    /**
     * Create an authorization instance for feature canManageGroup.
     *
     * @param user the user to which the authorization applies.
     * @param groupRest the resource to which the authorization applies.
     * @return the authorization.
     */
    private Authorization getCanManageGroupAuthorization(
        EPerson user, GroupRest groupRest) {
        return new Authorization(
            user, canManageGroupFeature, groupRest);
    }

    /**
     * Get the uri that points to the provided group.
     *
     * @param groupRest the REST representation of the group.
     * @return the uri that points to the group.
     */
    private String getGroupLink(GroupRest groupRest) {
        return utils.linkToSingleResource(
            groupRest, "self").getHref();
    }

    /**
     * Assert that the provided user has permission to manage the
     * provided group.
     *
     * @param user the user.
     * @param group the group.
     */
    private void canManageGroup(EPerson user, Group group)
        throws Exception {
        String token = getCachedAuthToken(user.getEmail());

        GroupRest groupRest = getGroupRest(group);
        Authorization authorization =
            getCanManageGroupAuthorization(user, groupRest);

        getClient(token).perform(
            get("/api/authz/authorizations/search/object")
                .param("uri", getGroupLink(groupRest))
                .param("feature", "canManageGroup")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$._embedded.authorizations",
                Matchers.containsInAnyOrder(
                    AuthorizationMatcher
                        .matchAuthorization(authorization)
                )));

        getClient(token).perform(
            get("/api/authz/authorizations/"
                    + "{epersonUuid}_canManageGroup"
                    + "_eperson.group_{groupUuid}",
                user.getID(), group.getID()
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                AuthorizationMatcher
                    .matchAuthorization(authorization)));
    }

    /**
     * Assert that the provided user does not have permission to manage
     * the provided group.
     *
     * @param user the user.
     * @param group the group.
     */
    private void canNotManageGroup(EPerson user, Group group)
        throws Exception {
        String token = getCachedAuthToken(user.getEmail());

        GroupRest groupRest = getGroupRest(group);

        getClient(token).perform(
            get("/api/authz/authorizations/search/object")
                .param("uri", getGroupLink(groupRest))
                .param("feature", "canManageGroup")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations")
                .doesNotExist());

        getClient(token).perform(
            get("/api/authz/authorizations/"
                    + "{epersonUuid}_canManageGroup"
                    + "_eperson.group_{groupUuid}",
                user.getID(), group.getID()
            )
        )
            .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Tests — one method per actor, batching all group assertions
    // -------------------------------------------------------------------------

    @Test
    public void testSiteAdminPermissions() throws Exception {
        canManageGroup(admin, community1AdminGroup);
        canManageGroup(admin, collection1AdminGroup);
        canManageGroup(admin, collection1SubmitterGroup);
        canManageGroup(admin, collection2AdminGroup);
        canManageGroup(admin, collection2SubmitterGroup);
        canManageGroup(admin, anonymousGroup);
        canManageGroup(admin, administratorGroup);
    }

    @Test
    public void testCommunity1AdminPermissions() throws Exception {
        canManageGroup(community1Admin, community1AdminGroup);
        canManageGroup(community1Admin, collection1AdminGroup);
        canManageGroup(community1Admin, collection1SubmitterGroup);
        canNotManageGroup(community1Admin, collection2AdminGroup);
        canNotManageGroup(
            community1Admin, collection2SubmitterGroup);
        canNotManageGroup(community1Admin, anonymousGroup);
        canNotManageGroup(community1Admin, administratorGroup);
    }

    @Test
    public void testCollection1AdminPermissions() throws Exception {
        canNotManageGroup(
            collection1Admin, community1AdminGroup);
        canManageGroup(collection1Admin, collection1AdminGroup);
        canManageGroup(
            collection1Admin, collection1SubmitterGroup);
        canNotManageGroup(
            collection1Admin, collection2AdminGroup);
        canNotManageGroup(
            collection1Admin, collection2SubmitterGroup);
        canNotManageGroup(collection1Admin, anonymousGroup);
        canNotManageGroup(collection1Admin, administratorGroup);
    }

    @Test
    public void testCollection1SubmitterPermissions()
        throws Exception {
        canNotManageGroup(
            collection1Submitter, community1AdminGroup);
        canNotManageGroup(
            collection1Submitter, collection1AdminGroup);
        canNotManageGroup(
            collection1Submitter, collection1SubmitterGroup);
        canNotManageGroup(
            collection1Submitter, collection2AdminGroup);
        canNotManageGroup(
            collection1Submitter, collection2SubmitterGroup);
        canNotManageGroup(collection1Submitter, anonymousGroup);
        canNotManageGroup(
            collection1Submitter, administratorGroup);
    }
}
