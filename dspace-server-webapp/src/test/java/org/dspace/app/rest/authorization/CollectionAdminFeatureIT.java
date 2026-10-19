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

public class CollectionAdminFeatureIT extends AbstractControllerIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private GroupService groupService;

    private Community topLevelCommunityA;
    private Community subCommunityA;
    private Collection collectionA;

    private EPerson topLevelCommunityAAdmin;
    private EPerson subCommunityAAdmin;
    private EPerson collectionAAdmin;
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
        collectionAAdmin = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("Jhon", "Brown")
            .withEmail("collectionAAdmin@my.edu")
            .withPassword(password)
            .build();
        collectionA = CollectionBuilder.createCollection(context, subCommunityA)
            .withName("The name of this collection is collectionA")
            .withAdminGroup(collectionAAdmin)
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
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .exists());
    }

    @Test
    public void testCommunityAdmin() throws Exception {
        String token = getAuthToken(topLevelCommunityAAdmin.getEmail(), password);

        // Verify the community admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .exists());
    }

    @Test
    public void testSubCommunityAdmin() throws Exception {
        String token = getAuthToken(subCommunityAAdmin.getEmail(), password);

        // Verify the subcommunity admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .exists());
    }

    @Test
    public void testCollectionAdmin() throws Exception {
        String token = getAuthToken(collectionAAdmin.getEmail(), password);

        // Verify the collection admin has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
        // Filter by "feature" ID because Admins may have >20 features enabled & cause this endpoint to paginate.
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()
            + "&feature=isCollectionAdmin"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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

        // Verify an ePerson in a subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .exists());
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
        // Filter by "feature" ID because Admins may have >20 features enabled & cause this endpoint to paginate.
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()
            + "&feature=isCollectionAdmin"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
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

        // Verify an ePerson in a sub-subgroup of a collection admin group has this feature
        getClient(token).perform(get("/api/authz/authorizations/search/object?embed=feature&uri="
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .exists());
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
            + "http://localhost/api/core/sites/" + siteService.findSite(context).getID()))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$._embedded.authorizations[?(@._embedded.feature.id=='isCollectionAdmin')]")
                    .doesNotExist());
    }
}
