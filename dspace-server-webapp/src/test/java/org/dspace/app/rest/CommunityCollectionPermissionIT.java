/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.workflow.WorkflowService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class CommunityCollectionPermissionIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ConfigurationService configurationService;

    Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void communityAdminAddMembersToCommunityAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                (org.springframework.data.rest.webmvc.RestMediaTypes
                     .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

    }

    @Test
    public void communityAdminRemoveMembersFromCommunityAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

    }

    @Test
    public void communityAdminAddChildGroupToCommunityAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

    }

    @Test
    public void communityAdminRemoveChildGroupFromCommunityAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));


        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

    }

    @Test
    public void communityAdminAddMembersToCommunityAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        configurationService.setProperty("core.authorization.community-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminRemoveMembersFromCommunityAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminAddChildGroupToCommunityAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        configurationService.setProperty("core.authorization.community-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()))
            .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminRemoveChildGroupFromCommunityAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.admin-group", true);
        context.restoreAuthSystemState();
    }







    @Test
    public void communityAdminAddMembersToCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

    }

    @Test
    public void communityAdminRemoveMembersFromCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

    }

    @Test
    public void communityAdminAddChildGroupToCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

    }

    @Test
    public void communityAdminRemoveChildGroupFromCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));


        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

    }

    @Test
    public void communityAdminAddMembersToCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminRemoveMembersFromCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminAddChildGroupToCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void communityAdminRemoveChildGroupFromCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();
    }







    @Test
    public void collectionAdminAddMembersToCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

    }

    @Test
    public void collectionAdminRemoveMembersFromCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

    }

    @Test
    public void collectionAdminAddChildGroupToCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

    }

    @Test
    public void collectionAdminRemoveChildGroupFromCollectionAdminGroupSuccess() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));


        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

    }

    @Test
    public void collectionAdminAddMembersToCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.not(Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void collectionAdminRemoveMembersFromCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        EPerson ePerson = EPersonBuilder.createEPerson(context).withEmail("testToAdd@test.com").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/epersons")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void collectionAdminAddChildGroupToCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.not(Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        ))));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();

    }

    @Test
    public void collectionAdminRemoveChildGroupFromCollectionAdminGroupPropertySetToFalse() throws Exception {

        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        Group group = GroupBuilder.createGroup(context).withName("testGroup").build();
        context.restoreAuthSystemState();


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(
            post("/api/eperson/groups/" + adminGroup.getID() + "/subgroups")
                .contentType(MediaType.parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);
        context.restoreAuthSystemState();

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        context.turnOffAuthorisationSystem();
        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", true);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", true);
        context.restoreAuthSystemState();
    }
}
