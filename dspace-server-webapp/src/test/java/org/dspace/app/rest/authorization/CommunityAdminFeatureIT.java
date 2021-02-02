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

import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityAdminFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    private Community topLevelCommunityA;
    private Community subCommunityA;
    private Community communityB;
    private Community communityC;
    private Collection collectionA;

    private EPerson topLevelCommunityAAdmin;
    private EPerson subCommunityAAdmin;
    private EPerson collectionAdmin;
    private EPerson submitter;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        topLevelCommunityAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("topLevelCommunityAAdmin@my.edu")
            .withPassword(password)
            .build();
        topLevelCommunityA = CommunityBuilder.createCommunity(context)
            .withName("The name of this community is topLevelCommunityA")
            .withAdminGroup(topLevelCommunityAAdmin)
            .build();

        subCommunityAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("subCommunityAAdmin@my.edu")
            .withPassword(password)
            .build();
        subCommunityA = CommunityBuilder.createCommunity(context)
            .withName("The name of this sub-community is subCommunityA")
            .withAdminGroup(subCommunityAAdmin)
            .addParentCommunity(context, topLevelCommunityA)
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
        collectionA = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("The name of this collection is collectionA")
            .withAdminGroup(collectionAdmin)
            .withSubmitterGroup(submitter)
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", "true");
    }

    @Test
    public void testAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // Verify the general admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testCommunityAdmin() throws Exception {
        String token = getAuthToken(topLevelCommunityAAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubCommunityAdmin() throws Exception {
        String token = getAuthToken(subCommunityAAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testCollectionAdmin() throws Exception {
        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    @Test
    public void testSubmitter() throws Exception {
        String token = getAuthToken(submitter.getEmail(), password);

        // Verify a submitter doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, Group.ADMIN))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of the site administrators has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfSubmitterGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, Group.ADMIN))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of the site administrators has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCollectionAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a collection admin group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfSubmitterGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/site/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCommunityAdmin')]")
                    .doesNotExist());
    }

    // findAdminAuthorized
    @Test
    public void testAdminSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(topLevelCommunityA)
            .withAction(Constants.ADMIN)
            .withUser(admin)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .withAdminGroup(admin)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // Verify the site admin gets all communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(topLevelCommunityA.getName(), topLevelCommunityA.getID(),
                    topLevelCommunityA.getHandle()),
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle()),
                CommunityMatcher.matchProperties(communityC.getName(), communityC.getID(), communityC.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));
    }

    @Test
    public void testCommunityAdminSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .withAdminGroup(topLevelCommunityAAdmin)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is named topLevelCommunityC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(topLevelCommunityAAdmin.getEmail(), password);

        // Verify the community admin gets all the communities he's admin for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(topLevelCommunityA.getName(), topLevelCommunityA.getID(),
                    topLevelCommunityA.getHandle()),
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubCommunityAdminSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .withAdminGroup(subCommunityAAdmin)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(subCommunityAAdmin.getEmail(), password);

        // Verify the community admin gets all the communities he's admin for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testCollectionAdminSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin doesn't have any matches for communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubmitterSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(submitter.getEmail(), password);

        // Verify the submitter doesn't have any matches for communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroupSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("adminSubGroup")
            .withParent(groupService.findByName(context, Group.ADMIN))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the site admins' subgroups members get all communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(topLevelCommunityA.getName(), topLevelCommunityA.getID(),
                    topLevelCommunityA.getHandle()),
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle()),
                CommunityMatcher.matchProperties(communityC.getName(), communityC.getID(), communityC.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));
    }

    @Test
    public void testSubGroupOfCommunityAdminGroupSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("communityAdminSubGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(communityB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COMMUNITY_" + topLevelCommunityA.getID() + "_ADMIN"))
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the community admins' subgroup users get all the communities he's admin for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(topLevelCommunityA.getName(), topLevelCommunityA.getID(),
                    topLevelCommunityA.getHandle()),
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroupSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("communityAdminSubGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(communityB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COMMUNITY_" + subCommunityA.getID() + "_ADMIN"))
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the sub-community admins' subgroup users get all the communities he's admin for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(subCommunityA.getName(), subCommunityA.getID(),
                    subCommunityA.getHandle()),
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities", Matchers.containsInAnyOrder(
                CommunityMatcher.matchProperties(communityB.getName(), communityB.getID(), communityB.getHandle())
            )));

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroupSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("collectionAdminSubGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        Collection collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionB)
            .withAction(Constants.ADMIN)
            .withGroup(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_ADMIN"))
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the collection admins' subgroup members don't have any matches for communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }

    @Test
    public void testSubGroupOfSubmitterGroupSearch() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("collectionAdminSubGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        communityB = CommunityBuilder.createCommunity(context)
            .withName("topLevelCommunityB is a very original name")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        Collection collectionB = CollectionBuilder.createCollection(context, communityB)
            .withName("collectionB")
            .build();
        ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collectionB)
            .withAction(Constants.ADD)
            .withGroup(groupService.findByName(context, "COLLECTION_" + collectionA.getID() + "_SUBMIT"))
            .build();
        communityC = CommunityBuilder.createCommunity(context)
            .withName("the last community is topLevelCommunityC")
            .addParentCommunity(context, topLevelCommunityA)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have any matches for communities
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify the search only shows dso's which according to the query
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityB.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());

        // Verify that a query doesn't show dso's which the user doesn't have rights for
        getClient(token).perform(get("/api/core/communities/search/findAdminAuthorized")
            .param("query", communityC.getName()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.communities").doesNotExist());
    }
}
