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

import org.dspace.app.rest.authorization.impl.ManageGroupFeature;
import org.dspace.app.rest.converter.GroupConverter;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ManageGroupFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private Utils utils;

    @Autowired
    private GroupConverter groupConverter;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    /**
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
     *       |_ (no people)
     *    |_ collection2
     *       |_ (g) collection2AdminGroup
     *          |_ (no people)
     *       |_ (g) collection2SubmitterGroup
     *          |_ (no people)
     * |_ community3
     *    |_ (no groups)
     *    |_ collection3
     *       |_ (no groups)
     */

    protected Community community1;
    protected Group community1AdminGroup;
    protected EPerson community1Admin;
    protected Collection collection1;
    protected Group collection1AdminGroup;
    protected EPerson collection1Admin;
    protected Group collection1SubmitterGroup;
    protected EPerson collection1Submitter;

    protected Community community2;
    protected Group community2AdminGroup;
    protected Collection collection2;
    protected Group collection2AdminGroup;
    protected Group collection2SubmitterGroup;

    protected Community community3;
    protected Collection collection3;

    protected Group anonymousGroup;
    protected Group administratorGroup;

    private AuthorizationFeature canManageGroupFeature;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        canManageGroupFeature = authorizationFeatureService.find(ManageGroupFeature.NAME);

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

        collection1 = CollectionBuilder.createCollection(context, community1)
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

        collection2 = CollectionBuilder.createCollection(context, community2)
            .withName("Collection 2")
            .withAdminGroup(eperson)
            .withSubmitterGroup(eperson)
            .build();

        collection2AdminGroup = collection2.getAdministrators();
        collection2SubmitterGroup = collection2.getSubmitters();


        community3 = CommunityBuilder.createCommunity(context)
            .withName("Community 3")
            .build();

        collection3 = CollectionBuilder.createCollection(context, community3)
            .withName("Collection 3")
            .build();


        anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        administratorGroup = groupService.findByName(context, Group.ADMIN);


        context.restoreAuthSystemState();
    }

    /**
     * Get the REST representation of the given group.
     * @param group the group.
     * @return the REST representation of the group.
     */
    protected GroupRest getGroupRest(Group group) throws Exception {
        return groupConverter.convert(context.reloadEntity(group), Projection.DEFAULT);
    }

    /**
     * Create an authorization instance for feature canManageGroup.
     * @param user the user to which the authorization applies.
     * @param groupRest the resource to which the authorization applies.
     * @return the authorization.
     */
    protected Authorization getCanManageGroupAuthorization(EPerson user, GroupRest groupRest) {
        return new Authorization(user, canManageGroupFeature, groupRest);
    }

    /**
     * Get the uri that points to the provided group.
     * @param groupRest the REST representation of the group.
     * @return the uri that points to the group.
     */
    protected String getGroupLink(GroupRest groupRest) {
        return utils.linkToSingleResource(groupRest, "self").getHref();
    }

    /**
     * Assert that the provided user has permission to manage the provided group.
     * @param user the user.
     * @param group the group.
     */
    protected void canManageGroup(EPerson user, Group group) throws Exception {
        String token = getAuthToken(user.getEmail(), password);

        GroupRest groupRest = getGroupRest(group);
        Authorization authorization = getCanManageGroupAuthorization(user, groupRest);

        getClient(token).perform(
            get("/api/authz/authorizations/search/object")
                .param("uri", getGroupLink(groupRest))
                .param("feature", "canManageGroup")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations", Matchers.containsInAnyOrder(
                AuthorizationMatcher.matchAuthorization(authorization)
            )));

        getClient(token).perform(
            get(
                "/api/authz/authorizations/{epersonUuid}_canManageGroup_eperson.group_{groupUuid}",
                user.getID(), group.getID()
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", AuthorizationMatcher.matchAuthorization(authorization)));
    }

    /**
     * Assert that the provided user does not have permission to manage the provided group.
     * @param user the user.
     * @param group the group.
     */
    protected void canNotManageGroup(EPerson user, Group group) throws Exception {
        String token = getAuthToken(user.getEmail(), password);

        GroupRest groupRest = getGroupRest(group);
        Authorization authorization = getCanManageGroupAuthorization(user, groupRest);

        getClient(token).perform(
            get("/api/authz/authorizations/search/object")
                .param("uri", getGroupLink(groupRest))
                .param("feature", "canManageGroup")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.authorizations").doesNotExist());

        getClient(token).perform(
            get(
                "/api/authz/authorizations/{epersonUuid}_canManageGroup_eperson.group_{groupUuid}",
                user.getID(), group.getID()
            )
        )
            .andExpect(status().isNotFound());
    }



    ////////////////
    // site admin //
    ////////////////

    @Test
    public void siteAdminCanManageCommunity1AdminGroup() throws Exception {
        canManageGroup(admin, community1AdminGroup);
    }

    @Test
    public void siteAdminCanManageCollection1AdminGroup() throws Exception {
        canManageGroup(admin, collection1AdminGroup);
    }

    @Test
    public void siteAdminCanManageCollection1SubmitterGroup() throws Exception {
        canManageGroup(admin, collection1SubmitterGroup);
    }

    @Test
    public void siteAdminCanManageCollection2AdminGroup() throws Exception {
        canManageGroup(admin, collection2AdminGroup);
    }

    @Test
    public void siteAdminCanManageCollection2SubmitterGroup() throws Exception {
        canManageGroup(admin, collection2SubmitterGroup);
    }

    @Test
    public void siteAdminCanManageAnonymousGroup() throws Exception {
        canManageGroup(admin, anonymousGroup);
    }

    @Test
    public void siteAdminCanManageAdministratorGroup() throws Exception {
        canManageGroup(admin, administratorGroup);
    }



    //////////////////////
    // community  admin //
    //////////////////////

    @Test
    public void community1AdminCanManageCommunity1AdminGroup() throws Exception {
        canManageGroup(community1Admin, community1AdminGroup);
    }

    @Test
    public void community1AdminCanManageCollection1AdminGroup() throws Exception {
        canManageGroup(community1Admin, collection1AdminGroup);
    }

    @Test
    public void community1AdminCanManageCollection1SubmitterGroup() throws Exception {
        canManageGroup(community1Admin, collection1SubmitterGroup);
    }

    @Test
    public void community1AdminCanNotManageCollection2AdminGroup() throws Exception {
        canNotManageGroup(community1Admin, collection2AdminGroup);
    }

    @Test
    public void community1AdminCanNotManageCollection2SubmitterGroup() throws Exception {
        canNotManageGroup(community1Admin, collection2SubmitterGroup);
    }

    @Test
    public void community1AdminCanNotManageAnonymousGroup() throws Exception {
        canNotManageGroup(community1Admin, anonymousGroup);
    }

    @Test
    public void community1AdminCanNotManageAdministratorGroup() throws Exception {
        canNotManageGroup(community1Admin, administratorGroup);
    }



    ////////////////////////
    // collection 1 admin //
    ////////////////////////

    @Test
    public void collection1AdminCanNotManageCommunity1AdminGroup() throws Exception {
        canNotManageGroup(collection1Admin, community1AdminGroup);
    }

    @Test
    public void collection1AdminCanManageCollection1AdminGroup() throws Exception {
        canManageGroup(collection1Admin, collection1AdminGroup);
    }

    @Test
    public void collection1AdminCanManageCollection1SubmitterGroup() throws Exception {
        canManageGroup(collection1Admin, collection1SubmitterGroup);
    }

    @Test
    public void collection1AdminCanNotManageCollection2AdminGroup() throws Exception {
        canNotManageGroup(collection1Admin, collection2AdminGroup);
    }

    @Test
    public void collection1AdminCanNotManageCollection2SubmitterGroup() throws Exception {
        canNotManageGroup(collection1Admin, collection2SubmitterGroup);
    }

    @Test
    public void collection1AdminCanNotManageAnonymousGroup() throws Exception {
        canNotManageGroup(collection1Admin, anonymousGroup);
    }

    @Test
    public void collection1AdminCanNotManageAdministratorGroup() throws Exception {
        canNotManageGroup(collection1Admin, administratorGroup);
    }



    ////////////////////////////
    // collection 1 submitter //
    ////////////////////////////

    @Test
    public void collection1SubmitterCanNotManageCommunity1AdminGroup() throws Exception {
        canNotManageGroup(collection1Submitter, community1AdminGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageCollection1AdminGroup() throws Exception {
        canNotManageGroup(collection1Submitter, collection1AdminGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageCollection1SubmitterGroup() throws Exception {
        canNotManageGroup(collection1Submitter, collection1SubmitterGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageCollection2AdminGroup() throws Exception {
        canNotManageGroup(collection1Submitter, collection2AdminGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageCollection2SubmitterGroup() throws Exception {
        canNotManageGroup(collection1Submitter, collection2SubmitterGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageAnonymousGroup() throws Exception {
        canNotManageGroup(collection1Submitter, anonymousGroup);
    }

    @Test
    public void collection1SubmitterCanNotManageAdministratorGroup() throws Exception {
        canNotManageGroup(collection1Submitter, administratorGroup);
    }

}
