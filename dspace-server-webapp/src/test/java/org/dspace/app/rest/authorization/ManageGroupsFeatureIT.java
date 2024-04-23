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

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ManageGroupsFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    private Community topLevelCommunity;
    private Community subCommunity;
    private Collection collection;

    private EPerson communityAdmin;
    private EPerson subCommunityAdmin;
    private EPerson collectionAdmin;
    private EPerson submitter;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

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

        configurationService.setProperty(
            "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", "true");
    }

    @Test
    public void testAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        // Verify the general admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCommunityAdmin() throws Exception {
        String token = getAuthToken(communityAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubCommunityAdmin() throws Exception {
        String token = getAuthToken(subCommunityAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCollectionAdmin() throws Exception {
        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubmitter() throws Exception {
        String token = getAuthToken(submitter.getEmail(), password);

        // Verify a submitter doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubmitterGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubCommunityAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCollectionAdminGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubmitterGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    // Disabled community configs
    @Test
    public void testAdminNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        String token = getAuthToken(admin.getEmail(), password);

        // Verify the general admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCommunityAdminNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        String token = getAuthToken(communityAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubCommunityAdminNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        String token = getAuthToken(subCommunityAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCollectionAdminNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubmitterNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        String token = getAuthToken(submitter.getEmail(), password);

        // Verify a submitter doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCommunityAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubmitterGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCommunityAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubCommunityAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCollectionAdminGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubmitterGroupNoCommunityGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    // Disabled collection configs
    @Test
    public void testAdminNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(admin.getEmail(), password);

        // Verify the general admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCommunityAdminNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(communityAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubCommunityAdminNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(subCommunityAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCollectionAdminNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubmitterNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(submitter.getEmail(), password);

        // Verify a submitter doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCommunityAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfSubmitterGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCommunityAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfSubCommunityAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCollectionAdminGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfSubmitterGroupNoCollectionGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    // Disabled community and collection configs
    @Test
    public void testAdminNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(admin.getEmail(), password);

        // Verify the general admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testCommunityAdminNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(communityAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubCommunityAdminNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(subCommunityAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testCollectionAdminNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(collectionAdmin.getEmail(), password);

        // Verify the collection admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubmitterNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        String token = getAuthToken(submitter.getEmail(), password);

        // Verify a submitter doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubGroupOfCommunityAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a community admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfSubCommunityAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a subcommunity admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfCollectionAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubGroupOfSubmitterGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        GroupBuilder.createGroup(context)
            .withName("userGroup")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a subgroup of submitter group doesn't have this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .exists());
    }

    @Test
    public void testSubSubGroupOfCommunityAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + topLevelCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfSubCommunityAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COMMUNITY_" + subCommunity.getID() + "_ADMIN"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfCollectionAdminGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_ADMIN"))
            .build();
        GroupBuilder.createGroup(context)
            .withName("GroupA")
            .withParent(groupB)
            .addMember(eperson)
            .build();
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        // Verify an ePerson in a sub-subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }

    @Test
    public void testSubSubGroupOfSubmitterGroupNoComColGroupPermission() throws Exception {
        configurationService.setProperty(
            "core.authorization.community-admin.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.admin-group", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.policies", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.submitters", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.workflows", "false");
        configurationService.setProperty(
            "core.authorization.community-admin.collection.admin-group", "false");

        context.turnOffAuthorisationSystem();
        Group groupB = GroupBuilder.createGroup(context)
            .withName("GroupB")
            .withParent(groupService.findByName(context, "COLLECTION_" + collection.getID() + "_SUBMIT"))
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='canManageGroups')]")
                    .doesNotExist());
    }
}
