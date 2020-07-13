/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.test.MetadataPatchSuite;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Jonas Van Goolen - (jonas@atmire.com)
 */
public class GroupRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ResourcePolicyService resourcePolicyService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private CollectionService collectionService;

    @Autowired
    private AuthorizeService authorizeService;

    Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        context.restoreAuthSystemState();
    }

    @Test
    public void createTest()
        throws Exception {

        // hold the id of the created workflow item
        AtomicReference<UUID> idRef = new AtomicReference<>();
        AtomicReference<UUID> idRefNoEmbeds = new AtomicReference<UUID>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            GroupRest groupRest = new GroupRest();
            GroupRest groupRestNoEmbeds = new GroupRest();
            String groupName = "testGroup1";
            String groupDescription = "test description";
            String groupNameNoEmbeds = "testGroup2";

            groupRest.setName(groupName);
            MetadataRest metadata = new MetadataRest();
            metadata.put("dc.description", new MetadataValueRest(groupDescription));
            groupRest.setMetadata(metadata);

            groupRestNoEmbeds.setName(groupNameNoEmbeds);

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(post("/api/eperson/groups")
                    .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType)
                    .param("projection", "full"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", GroupMatcher.matchFullEmbeds()))
                    .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                    );

            getClient(authToken).perform(get("/api/eperson/groups"))
                       //The status has to be 200 OK
                       .andExpect(status().isOk())
                       .andExpect(content().contentType(contentType))
                       .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                               GroupMatcher.matchGroupWithName(groupName),
                               GroupMatcher.matchGroupWithName("Administrator"),
                               GroupMatcher.matchGroupWithName("Anonymous"))));

            getClient(authToken).perform(post("/api/eperson/groups")
                    .content(mapper.writeValueAsBytes(groupRestNoEmbeds)).contentType(contentType))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                    .andDo(result -> idRefNoEmbeds
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id"))));

            GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
            Group group = groupService.find(context, idRef.get());

            assertEquals(
                    groupService.getMetadata(group, "dc.description"),
                    groupDescription
            );

        } finally {
            // remove the created group if any
            GroupBuilder.deleteGroup(idRef.get());
            GroupBuilder.deleteGroup(idRefNoEmbeds.get());
        }
    }

    @Test
    public void createUnauthauthorizedTest()
            throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        String groupName = "testGroupUnauth1";

        groupRest.setName(groupName);

        getClient().perform(post("/api/eperson/groups")
                .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void createForbiddenTest()
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        String groupName = "testGroupForbidden1";

        groupRest.setName(groupName);

        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(post("/api/eperson/groups")
                .content(mapper.writeValueAsBytes(groupRest)).contentType(contentType))
                .andExpect(status().isForbidden());
    }

    @Test
    public void createUnprocessableTest()
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(post("/api/eperson/groups")
                        .content(mapper.writeValueAsBytes(groupRest))
                        .contentType(contentType))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findAllTest() throws Exception {

        getClient().perform(get("/api/eperson/groups"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   //The array of groups should have a size 2
                   .andExpect(jsonPath("$._embedded.groups", hasSize(2)))
                   // The default groups should consist of "Anonymous" and "Anonymous"
                   .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                       GroupMatcher.matchGroupWithName("Administrator"),
                       GroupMatcher.matchGroupWithName("Anonymous")
                   )))
        ;
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/eperson/groups"))
                   .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginationTest() throws Exception {

        getClient().perform(get("/api/eperson/groups"))
                   //The status has to be 403 Not Authorized
                   .andExpect(status().isUnauthorized());


        String token = getAuthToken(admin.getEmail(), password);

        //When we call the root endpoint
        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(2)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)));
    }


    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                                  .withName(testGroupName)
                                  .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        // When full projection is requested, response should include expected properties, links, and embeds.
        String generatedGroupId = group.getID().toString();
        String groupIdCall = "/api/eperson/groups/" + generatedGroupId;
        getClient(token).perform(get(groupIdCall).param("projection", "full"))
        //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", GroupMatcher.matchFullEmbeds()))
                .andExpect(jsonPath("$", GroupMatcher.matchLinks(group.getID())))
                   .andExpect(jsonPath("$", Matchers.is(
                       GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                   )))
        ;

        getClient(token).perform(get("/api/eperson/groups"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
        ;

        // When no projection is requested, response should include expected properties, links, and no embeds.
        getClient(token).perform(get("/api/eperson/groups/"  + generatedGroupId))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void readGroupAuthorizationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group = GroupBuilder.createGroup(context)
                                  .withName("Group1")
                                  .build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Group2")
                                   .addMember(eperson)
                                   .build();

        context.restoreAuthSystemState();

        //Admin can access
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + group2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                GroupMatcher.matchGroupEntry(group2.getID(), group2.getName())
                        )))
                        .andExpect(jsonPath("$", Matchers.not(
                                Matchers.is(
                                        GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                                )
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                        Matchers.containsString("/api/eperson/groups/" + group2.getID())));


        //People in group should be able to access token
        token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/eperson/groups/" + group2.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$", Matchers.is(
                                GroupMatcher.matchGroupEntry(group2.getID(), group2.getName())
                        )))
                        .andExpect(jsonPath("$", Matchers.not(
                                Matchers.is(
                                        GroupMatcher.matchGroupEntry(group.getID(), group.getName())
                                )
                        )))
                        .andExpect(jsonPath("$._links.self.href",
                                        Matchers.containsString("/api/eperson/groups/" + group2.getID())));
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group privateGroup = GroupBuilder.createGroup(context)
                .withName("Private Group")
                .build();

        resourcePolicyService.removePolicies(context, privateGroup, Constants.READ);
        context.restoreAuthSystemState();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/eperson/groups/" + privateGroup.getID()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                                  .withName(testGroupName)
                                  .build();

        context.restoreAuthSystemState();

        String generatedGroupId = group.getID().toString();
        String groupIdCall = "/api/eperson/groups/" + UUID.randomUUID();
        getClient().perform(get(groupIdCall))
                   //The status has to be 200 OK
                   .andExpect(status().isNotFound())
        ;
    }

    @Test
    public void searchMethodsExist() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons"))
                            .andExpect(jsonPath("$._links.search.href", Matchers.notNullValue()));

        getClient(authToken).perform(get("/api/eperson/epersons/search"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._links.byMetadata", Matchers.notNullValue()));
    }

    @Test
    public void findByMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context)
                                   .withName("Test group")
                                   .build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2")
                                   .build();

        Group group3 = GroupBuilder.createGroup(context)
                                   .withName("Test group 3")
                                   .build();

        Group group4 = GroupBuilder.createGroup(context)
                                   .withName("Test other group")
                                   .build();


        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/groups/search/byMetadata")
                                             .param("query", group1.getName()))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                                    GroupMatcher.matchGroupEntry(group1.getID(), group1.getName()),
                                    GroupMatcher.matchGroupEntry(group2.getID(), group2.getName()),
                                    GroupMatcher.matchGroupEntry(group3.getID(), group3.getName())
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(3)));

        // it must be case insensitive
        getClient(authToken).perform(get("/api/eperson/groups/search/byMetadata")
                                             .param("query", String.valueOf(group1.getID())))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.groups", Matchers.contains(
                                    GroupMatcher.matchGroupEntry(group1.getID(), group1.getName())
                            )))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByMetadataUnauthorized() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient().perform(get("/api/eperson/groups/search/byMetadata")
                                    .param("query", "Administrator"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByMetadataForbidden() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/groups/search/byMetadata")
                                             .param("query", "Administrator"))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void findByMetadataUndefined() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/groups/search/byMetadata")
                                             .param("query", "Non-existing Group"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByMetadataMissingParameter() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/groups/search/byMetadata"))
                            .andExpect(status().isBadRequest());
    }


    @Test
    public void patchGroupMetadataAuthorized() throws Exception {
        runPatchMetadataTests(admin, 200);
    }

    @Test
    public void patchGroupMetadataUnauthorized() throws Exception {
        runPatchMetadataTests(eperson, 403);
    }

    private void runPatchMetadataTests(EPerson asUser, int expectedStatus) throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context).withName("Group").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(asUser.getEmail(), password);

        new MetadataPatchSuite().runWith(getClient(token), "/api/eperson/groups/" + group.getID(), expectedStatus);
    }

    @Test
    public void patchGroupName() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = GroupBuilder.createGroup(context).withName("Group").build();
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", "new name");
        ops.add(replaceOperation);
        String requestBody = getPatchContent(ops);
        getClient(token)
                .perform(patch("/api/eperson/groups/" + group.getID()).content(requestBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());
        getClient(token)
                .perform(get("/api/eperson/groups/" + group.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        GroupMatcher.matchGroupEntry(group.getID(), "new name"))
                ));
    }

    @Test
    public void patchGroupWithParentUnprocessable() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .withWorkflowGroup(1, admin, reviewer1)
                .build();

        final Group workflowGroup = col1.getWorkflowStep1(context);
        final String name = workflowGroup.getName();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", "new name");
        ops.add(replaceOperation);
        String requestBody = getPatchContent(ops);
        getClient(token)
                .perform(patch("/api/eperson/groups/" + workflowGroup.getID()).content(requestBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        getClient(token)
                .perform(get("/api/eperson/groups/" + workflowGroup.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        GroupMatcher.matchGroupEntry(workflowGroup.getID(), name))
                ));
    }

    @Test
    public void patchPermanentGroupUnprocessable() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        final Group group = groupService.findByName(context, Group.ANONYMOUS);
        final String name = group.getName();
        String token = getAuthToken(admin.getEmail(), password);

        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", "new name");
        ops.add(replaceOperation);
        String requestBody = getPatchContent(ops);
        getClient(token)
                .perform(patch("/api/eperson/groups/" + group.getID()).content(requestBody)
                                 .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isUnprocessableEntity());

        getClient(token)
                .perform(get("/api/eperson/groups/" + group.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.is(
                        GroupMatcher.matchGroupEntry(group.getID(), name))
                ));
    }

    @Test
    public void addChildGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            assertTrue(
                    groupService.isMember(parentGroup, childGroup1)
            );

            assertTrue(
                    groupService.isMember(parentGroup, childGroup2)
            );

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addChildGroupCommunityAdminTest() throws Exception {

        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Community community = null;

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            community = communityService.create(null, context);
            parentGroup = communityService.createAdministrators(context, community);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            groupService.addMember(context, parentGroup, eperson);
            groupService.update(context, parentGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            assertTrue(
                    groupService.isMember(parentGroup, childGroup1)
            );

            assertTrue(
                    groupService.isMember(parentGroup, childGroup2)
            );

        } finally {
            if (community != null) {
                CommunityBuilder.deleteCommunity(community.getID());
            }
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addChildGroupForbiddenTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isForbidden());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addChildGroupUnauthorizedTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            getClient().perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isUnauthorized());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addChildGroupNotFoundTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + UUID.randomUUID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isNotFound());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addChildGroupUnprocessableTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup1 = null;
        Group childGroup2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup1 = groupService.create(context);
            childGroup2 = groupService.create(context);

            groupService.addMember(context, childGroup1, parentGroup);
            groupService.update(context, childGroup1);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup1 = context.reloadEntity(childGroup1);
            childGroup2 = context.reloadEntity(childGroup2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/123456789\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isUnprocessableEntity());

            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/subgroups")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + childGroup1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + childGroup2.getID()
                            )
            ).andExpect(status().isUnprocessableEntity());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup1 != null) {
                GroupBuilder.deleteGroup(childGroup1.getID());
            }
            if (childGroup2 != null) {
                GroupBuilder.deleteGroup(childGroup2.getID());
            }
        }
    }

    @Test
    public void addMemberTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + member1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            assertTrue(
                    groupService.isMember(context, member1, parentGroup)
            );

            assertTrue(
                    groupService.isMember(context, member2, parentGroup)
            );

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void addMemberCommunityAdminTest() throws Exception {

        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Community community = null;
        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            community = communityService.create(null, context);
            parentGroup = communityService.createAdministrators(context, community);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            groupService.addMember(context, parentGroup, eperson);
            groupService.update(context, parentGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + member1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            assertTrue(
                    groupService.isMember(context, member1, parentGroup)
            );

            assertTrue(
                    groupService.isMember(context, member2, parentGroup)
            );

        } finally {
            if (community != null) {
                CommunityBuilder.deleteCommunity(community.getID());
            }
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void addMemberForbiddenTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + member1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isForbidden());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void addMemberUnauthorizedTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            getClient().perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + member1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isUnauthorized());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void addMemberNotFoundTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    post("/api/eperson/groups/" + UUID.randomUUID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/" + member1.getID() + "/\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isNotFound());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void addMemberUnprocessableTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member1 = null;
        EPerson member2 = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member1 = ePersonService.create(context);
            member2 = ePersonService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member1 = context.reloadEntity(member1);
            member2 = context.reloadEntity(member2);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    post("/api/eperson/groups/" + parentGroup.getID() + "/epersons")
                            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                            .content(REST_SERVER_URL + "eperson/groups/123456789\n"
                                    + REST_SERVER_URL + "eperson/groups/" + member2.getID()
                            )
            ).andExpect(status().isUnprocessableEntity());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member1 != null) {
                EPersonBuilder.deleteEPerson(member1.getID());
            }
            if (member2 != null) {
                EPersonBuilder.deleteEPerson(member2.getID());
            }
        }
    }

    @Test
    public void removeChildGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup = groupService.create(context);
            groupService.addMember(context, parentGroup, childGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/subgroups/" + childGroup.getID())
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            assertFalse(
                    groupService.isMember(parentGroup, childGroup)
            );

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeChildGroupCommunityAdminTest() throws Exception {

        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Community community = null;
        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            community = communityService.create(null, context);
            parentGroup = communityService.createAdministrators(context, community);
            childGroup = groupService.create(context);

            groupService.addMember(context, parentGroup, childGroup);
            groupService.addMember(context, parentGroup, eperson);
            groupService.update(context, parentGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/subgroups/" + childGroup.getID())
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            assertFalse(
                    groupService.isMember(parentGroup, childGroup)
            );

        } finally {
            if (community != null) {
                CommunityBuilder.deleteCommunity(community.getID());
            }
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeChildGroupForbiddenTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/subgroups/" + childGroup.getID())
            ).andExpect(status().isForbidden());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeChildGroupUnauthorizedTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup = groupService.create(context);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            getClient().perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/subgroups/" + childGroup.getID())
            ).andExpect(status().isUnauthorized());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeChildGroupNotFoundTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup = groupService.create(context);

            groupService.addMember(context, childGroup, parentGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + UUID.randomUUID() + "/subgroups/" + childGroup.getID())
            ).andExpect(status().isNotFound());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeChildGroupUnprocessableTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        Group parentGroup = null;
        Group childGroup = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            childGroup = groupService.create(context);

            groupService.addMember(context, childGroup, parentGroup);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            childGroup = context.reloadEntity(childGroup);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/subgroups/" + UUID.randomUUID())
            ).andExpect(status().isUnprocessableEntity());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (childGroup != null) {
                GroupBuilder.deleteGroup(childGroup.getID());
            }
        }
    }

    @Test
    public void removeMemberTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member = ePersonService.create(context);
            groupService.addMember(context, parentGroup, member);
            assertTrue(groupService.isMember(context, member, parentGroup));

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/epersons/" + member.getID())
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            assertFalse(
                    groupService.isMember(context, member, parentGroup)
            );

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void removeMemberCommunityAdminTest() throws Exception {

        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Community community = null;
        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            community = communityService.create(null, context);
            parentGroup = communityService.createAdministrators(context, community);
            member = ePersonService.create(context);

            groupService.addMember(context, parentGroup, member);
            groupService.addMember(context, parentGroup, eperson);
            groupService.update(context, parentGroup);

            assertTrue(groupService.isMember(context, member, parentGroup));

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/epersons/" + member.getID())
            ).andExpect(status().isNoContent());

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            assertFalse(
                    groupService.isMember(context, member, parentGroup)
            );

        } finally {
            if (community != null) {
                CommunityBuilder.deleteCommunity(community.getID());
            }
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void removeMemberForbiddenTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member = ePersonService.create(context);
            groupService.addMember(context, parentGroup, member);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(eperson.getEmail(), password);
            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/epersons/" + member.getID())
            ).andExpect(status().isForbidden());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void removeMemberUnauthorizedTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member = ePersonService.create(context);
            groupService.addMember(context, parentGroup, member);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            getClient().perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/epersons/" + member.getID())
            ).andExpect(status().isUnauthorized());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void removeMemberNotFoundTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member = ePersonService.create(context);
            groupService.addMember(context, parentGroup, member);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + UUID.randomUUID() + "/epersons/" + member.getID())
            ).andExpect(status().isNotFound());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void removeMemberUnprocessableTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        Group parentGroup = null;
        EPerson member = null;

        try {
            context.turnOffAuthorisationSystem();

            parentGroup = groupService.create(context);
            member = ePersonService.create(context);
            groupService.addMember(context, parentGroup, member);

            context.commit();

            parentGroup = context.reloadEntity(parentGroup);
            member = context.reloadEntity(member);

            context.restoreAuthSystemState();
            String authToken = getAuthToken(admin.getEmail(), password);

            getClient(authToken).perform(
                    delete("/api/eperson/groups/" + parentGroup.getID() + "/epersons/" + UUID.randomUUID())
            ).andExpect(status().isUnprocessableEntity());

        } finally {
            if (parentGroup != null) {
                GroupBuilder.deleteGroup(parentGroup.getID());
            }
            if (member != null) {
                EPersonBuilder.deleteEPerson(member.getID());
            }
        }
    }

    @Test
    public void deleteGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group parentGroup = GroupBuilder.createGroup(context)
                .withName("test group")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isNoContent());

        getClient(authToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void deleteGroupUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group parentGroup = GroupBuilder.createGroup(context)
                .withName("test group")
                .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isOk());

        getClient().perform(
                delete("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isUnauthorized());

        getClient(authToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isOk());
    }

    @Test
    public void deleteGroupUnprocessableTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson reviewer1 = EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                .withName("Collection 1")
                .withWorkflowGroup(1, admin, reviewer1)
                .build();
        Group workflowGroup = col1.getWorkflowStep1(context);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/groups/" + workflowGroup.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/groups/" + workflowGroup.getID())
        ).andExpect(status().isUnprocessableEntity());

        getClient(authToken).perform(
                get("/api/eperson/groups/" + workflowGroup.getID())
        ).andExpect(status().isOk());
    }

    @Test
    public void deletePermanentGroupUnprocessableTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        final Group group = groupService.findByName(context, Group.ANONYMOUS);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                get("/api/eperson/groups/" + group.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/groups/" + group.getID())
        ).andExpect(status().isUnprocessableEntity());

        getClient(authToken).perform(
                get("/api/eperson/groups/" + group.getID())
        ).andExpect(status().isOk());

    }

    @Test
    public void deleteGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group parentGroup = GroupBuilder.createGroup(context)
                .withName("test group")
                .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(adminToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isOk());

        getClient(authToken).perform(
                delete("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isForbidden());

        getClient(adminToken).perform(
                get("/api/eperson/groups/" + parentGroup.getID())
        ).andExpect(status().isOk());
    }

    @Test
    public void deleteGroupNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(
                delete("/api/eperson/groups/" + UUID.randomUUID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void getGroupObjectCommunityTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(admin)
                .build();
        Group adminGroup = community.getAdministrators();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                get("/api/eperson/groups/" + adminGroup.getID() + "/object")
        ).andExpect(status().isOk());
    }

    @Test
    public void getGroupObjectCollectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(admin)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                .withName("Collection")
                .withAdminGroup(admin)
                .withWorkflowGroup(1, admin)
                .withSubmitterGroup(admin)
                .build();
        Group adminGroup = collection.getAdministrators();
        Group worfklowGroup = collection.getWorkflowStep1(context);
        Group submitterGroup = collection.getSubmitters();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                get("/api/eperson/groups/" + adminGroup.getID() + "/object")
        ).andExpect(status().isOk());
        getClient(authToken).perform(
                get("/api/eperson/groups/" + worfklowGroup.getID() + "/object")
        ).andExpect(status().isOk());
        getClient(authToken).perform(
                get("/api/eperson/groups/" + submitterGroup.getID() + "/object")
        ).andExpect(status().isOk());
    }

    @Test
    public void getGroupObjectNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = GroupBuilder.createGroup(context)
                .withName("test group")
                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(
                get("/api/eperson/groups/" + adminGroup.getID() + "/object")
        ).andExpect(status().isNoContent());
    }

    @Test
    public void getGroupObjectUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .withAdminGroup(admin)
                .build();
        Group adminGroup = community.getAdministrators();
        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/eperson/groups/" + adminGroup.getID() + "/object")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    public void patchNameTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String testGroupName = "Test group";
        Group group = GroupBuilder.createGroup(context)
                .withName(testGroupName)
                .build();
        context.restoreAuthSystemState();

        String newName = "New test name";
        List<Operation> ops = new ArrayList<>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", newName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String token = getAuthToken(admin.getEmail(), password);

        // updates email
        getClient(token).perform(
                patch("/api/eperson/groups/" + group.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(jsonPath("$.name", Matchers.is(newName)))
                .andExpect(status().isOk());
    }

    @Test
    public void findByMetadataByCommAdminAndByColAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson colSubmitter = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("colSubmitter@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(colSubmitter)
                                           .build();

        Group group1 = GroupBuilder.createGroup(context)
                                   .withName("Test group")
                                   .build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2")
                                   .build();

        Group group3 = GroupBuilder.createGroup(context)
                                   .withName("Test group 3")
                                   .build();

        Group group4 = GroupBuilder.createGroup(context)
                                   .withName("Test other group")
                                   .build();

        context.restoreAuthSystemState();

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);
        String tokenAdminCol = getAuthToken(adminCol1.getEmail(), password);
        String tokenSubmitterCol = getAuthToken(colSubmitter.getEmail(), password);

        getClient(tokenAdminComm).perform(get("/api/eperson/groups/search/byMetadata")
                .param("query", group1.getName()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.groups",Matchers.containsInAnyOrder(
                           GroupMatcher.matchGroupEntry(group1.getID(), group1.getName()),
                           GroupMatcher.matchGroupEntry(group2.getID(), group2.getName()),
                           GroupMatcher.matchGroupEntry(group3.getID(), group3.getName()))))
                .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient(tokenAdminCol).perform(get("/api/eperson/groups/search/byMetadata")
                .param("query", group1.getName()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.groups", Matchers.containsInAnyOrder(
                           GroupMatcher.matchGroupEntry(group1.getID(), group1.getName()),
                           GroupMatcher.matchGroupEntry(group2.getID(), group2.getName()),
                           GroupMatcher.matchGroupEntry(group3.getID(), group3.getName()))))
                .andExpect(jsonPath("$.page.totalElements", is(3)));

        getClient(tokenSubmitterCol).perform(get("/api/eperson/groups/search/byMetadata")
                .param("query", group1.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findByMetadataByCommAdminAndByColAdminWithoutAuthorizationsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        List<String> confPropsCollectionAdmins = new LinkedList<>();
        confPropsCollectionAdmins.add("core.authorization.collection-admin.policies");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.submitters");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.workflows");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.admin-group");

        List<String> confPropsCommunityAdmins = new LinkedList<>();
        confPropsCommunityAdmins.add("core.authorization.community-admin.policies");
        confPropsCommunityAdmins.add("core.authorization.community-admin.admin-group");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.policies");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.workflows");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.submitters");
        confPropsCommunityAdmins.add("core.authorization.community-admin.collection.admin-group");

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .build();

        Group group1 = GroupBuilder.createGroup(context)
                                   .withName("Test group")
                                   .build();

        Group group2 = GroupBuilder.createGroup(context)
                                   .withName("Test group 2")
                                   .build();

        Group group3 = GroupBuilder.createGroup(context)
                                   .withName("Test group 3")
                                   .build();

        Group group4 = GroupBuilder.createGroup(context)
                                   .withName("Test other group")
                                   .build();

        context.restoreAuthSystemState();

        String tokenAdminCol = getAuthToken(adminCol1.getEmail(), password);
        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);

        for (String prop : confPropsCollectionAdmins) {
            getClient(tokenAdminCol).perform(get("/api/eperson/groups/search/byMetadata")
                    .param("query", group1.getName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.groups",Matchers.containsInAnyOrder(
                               GroupMatcher.matchGroupEntry(group1.getID(), group1.getName()),
                               GroupMatcher.matchGroupEntry(group2.getID(), group2.getName()),
                               GroupMatcher.matchGroupEntry(group3.getID(), group3.getName()))))
                    .andExpect(jsonPath("$.page.totalElements", is(3)));

            configurationService.setProperty(prop, false);
        }

        getClient(tokenAdminCol).perform(get("/api/eperson/groups/search/byMetadata")
                .param("query", group1.getName()))
                .andExpect(status().isForbidden());

        for (String prop : confPropsCommunityAdmins) {
            getClient(tokenAdminComm).perform(get("/api/eperson/groups/search/byMetadata")
                    .param("query", group1.getName()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$._embedded.groups",Matchers.containsInAnyOrder(
                               GroupMatcher.matchGroupEntry(group1.getID(), group1.getName()),
                               GroupMatcher.matchGroupEntry(group2.getID(), group2.getName()),
                               GroupMatcher.matchGroupEntry(group3.getID(), group3.getName()))))
                    .andExpect(jsonPath("$.page.totalElements", is(3)));

            configurationService.setProperty(prop, false);
        }

        getClient(tokenAdminCol).perform(get("/api/eperson/groups/search/byMetadata")
                .param("query", group1.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void commAdminManageOwnerAdminGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();

        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();


        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Group groupAdmins = child1.getAdministrators();

        context.restoreAuthSystemState();

        String tokenCommAdmin = getAuthToken(adminChild1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, groupAdmins));

        getClient(tokenCommAdmin).perform(post("/api/eperson/groups/" + groupAdmins.getID() + "/epersons")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()
                        ))
               .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter1, groupAdmins));

        getClient(tokenCommAdmin).perform(delete("/api/eperson/groups/"
                                          + groupAdmins.getID() + "/epersons/" + submitter1.getID()))
                                 .andExpect(status().isNoContent());

        assertFalse(groupService.isMember(context, submitter1, groupAdmins));
    }

    @Test
    public void colAdminManageSubmitterGroupAndAdminGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();
        EPerson submitter3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jack", "Brown")
                .withEmail("submitter3@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(eperson)
                                           .build();

        Group groupSubmitters = col1.getSubmitters();
        Group groupAdmins = col1.getAdministrators();

        context.restoreAuthSystemState();

        String tokenAdminCol = getAuthToken(adminCol1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, groupSubmitters));
        assertFalse(groupService.isMember(context, submitter2, groupSubmitters));

        getClient(tokenAdminCol).perform(post("/api/eperson/groups/" + groupSubmitters.getID() + "/epersons")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID() + "/\n"
                               + REST_SERVER_URL + "eperson/groups/" + submitter2.getID()
                        ))
               .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter1, groupSubmitters));
        assertTrue(groupService.isMember(context, submitter2, groupSubmitters));

        assertFalse(groupService.isMember(context, submitter3, groupAdmins));

        getClient(tokenAdminCol).perform(
                post("/api/eperson/groups/" + groupAdmins.getID() + "/epersons")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + submitter3.getID()
                        ))
               .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter3, groupAdmins));
    }

    @Test
    public void colAdminWithoutRightsTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        List<String> confPropsCollectionAdmins = new LinkedList<>();
        confPropsCollectionAdmins.add("core.authorization.collection-admin.policies");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.submitters");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.workflows");
        confPropsCollectionAdmins.add("core.authorization.collection-admin.admin-group");

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();
        EPerson submitter3 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Jack", "Brown")
                .withEmail("submitter3@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(submitter2)
                                           .build();

        Group groupSubmitters = col1.getSubmitters();

        context.restoreAuthSystemState();

        String tokenAdminCol = getAuthToken(adminCol1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, groupSubmitters));

        getClient(tokenAdminCol).perform(
                post("/api/eperson/groups/" + groupSubmitters.getID() + "/epersons")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()
                        ))
               .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter1, groupSubmitters));

        for (String prop : confPropsCollectionAdmins) {
            configurationService.setProperty(prop, false);
        }

        assertFalse(groupService.isMember(context, submitter3, groupSubmitters));

        getClient(tokenAdminCol).perform(post("/api/eperson/groups/" + groupSubmitters.getID() + "/epersons")
                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                 .content(REST_SERVER_URL + "eperson/groups/" + submitter3.getID()
                 ))
                 .andExpect(status().isForbidden());

        assertFalse(groupService.isMember(context, submitter3, groupSubmitters));
    }

    @Test
    public void communityAdminCanManageCollectionSubmittersGroupAndAdminsGroupsTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(eperson)
                                           .build();

        Group groupSubmitters = col1.getSubmitters();
        Group groupAdministrators = col1.getAdministrators();

        context.restoreAuthSystemState();

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, groupSubmitters));
        assertFalse(groupService.isMember(context, submitter2, groupSubmitters));

        getClient(tokenAdminComm).perform(post("/api/eperson/groups/" + groupSubmitters.getID() + "/epersons")
                 .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                 .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID() + "/\n"
                        + REST_SERVER_URL + "eperson/groups/" + submitter2.getID()
                         ))
                 .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter1, groupSubmitters));
        assertTrue(groupService.isMember(context, submitter2, groupSubmitters));

        getClient(tokenAdminComm).perform(delete("/api/eperson/groups/"
                                          + groupSubmitters.getID() + "/epersons/" + submitter1.getID()))
                                 .andExpect(status().isNoContent());

        assertFalse(groupService.isMember(context, submitter1, groupSubmitters));
        assertTrue(groupService.isMember(context, submitter2, groupSubmitters));

        getClient(tokenAdminComm).perform(post("/api/eperson/groups/" + groupAdministrators.getID() + "/epersons")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()
                        ))
                .andExpect(status().isNoContent());

        assertTrue(groupService.isMember(context, submitter1, groupAdministrators));
        assertTrue(groupService.isMember(context, adminCol1, groupAdministrators));
        getClient(tokenAdminComm).perform(delete("/api/eperson/groups/"
                                          + groupAdministrators.getID() + "/epersons/" + adminCol1.getID()))
                 .andExpect(status().isNoContent());

        assertFalse(groupService.isMember(context, adminCol1, groupAdministrators));

    }


    @Test
    public void commAdminAndColAdminCanManageItemReadGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(eperson)
                                           .build();

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;
        Group itemReadGroup = collectionService.createDefaultReadGroup(context, col1, itemGroupString, defaultItemRead);

        context.restoreAuthSystemState();

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);
        String tokenAdminCol = getAuthToken(adminChild1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, itemReadGroup));
        assertFalse(groupService.isMember(context, submitter2, itemReadGroup));

        getClient(tokenAdminCol).perform(post("/api/eperson/groups/" + itemReadGroup.getID() + "/epersons")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()))
                .andExpect(status().isNoContent());

       assertTrue(groupService.isMember(context, submitter1, itemReadGroup));


       getClient(tokenAdminComm).perform(post("/api/eperson/groups/" + itemReadGroup.getID() + "/epersons")
               .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
               .content(REST_SERVER_URL + "eperson/groups/" + submitter2.getID()))
               .andExpect(status().isNoContent());

      assertTrue(groupService.isMember(context, submitter2, itemReadGroup));

       getClient(tokenAdminComm).perform(delete("/api/eperson/groups/"
                                         + itemReadGroup.getID() + "/epersons/" + submitter2.getID()))
                                .andExpect(status().isNoContent());

       assertFalse(groupService.isMember(context, submitter2, itemReadGroup));

       getClient(tokenAdminCol).perform(delete("/api/eperson/groups/"
                                         + itemReadGroup.getID() + "/epersons/" + submitter1.getID()))
                                .andExpect(status().isNoContent());

      assertFalse(groupService.isMember(context, submitter1, itemReadGroup));

    }

    @Test
    public void commAdminAndColAdminCanManageBitstreamReadGroupTest() throws Exception {

        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withSubmitterGroup(eperson)
                                           .build();

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group bitstreamReadGroup = collectionService.createDefaultReadGroup(context, col1, bitstreamGroupString,
                                                                                            defaultBitstreamRead);

        context.restoreAuthSystemState();

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);
        String tokenAdminCol = getAuthToken(adminChild1.getEmail(), password);

        assertFalse(groupService.isMember(context, submitter1, bitstreamReadGroup));
        assertFalse(groupService.isMember(context, submitter2, bitstreamReadGroup));

        getClient(tokenAdminCol).perform(post("/api/eperson/groups/" + bitstreamReadGroup.getID() + "/epersons")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()))
                .andExpect(status().isNoContent());

       assertTrue(groupService.isMember(context, submitter1, bitstreamReadGroup));


       getClient(tokenAdminComm).perform(post("/api/eperson/groups/" + bitstreamReadGroup.getID() + "/epersons")
               .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
               .content(REST_SERVER_URL + "eperson/groups/" + submitter2.getID()))
               .andExpect(status().isNoContent());

      assertTrue(groupService.isMember(context, submitter2, bitstreamReadGroup));

       getClient(tokenAdminComm).perform(delete("/api/eperson/groups/"
                                         + bitstreamReadGroup.getID() + "/epersons/" + submitter2.getID()))
                                .andExpect(status().isNoContent());

       assertFalse(groupService.isMember(context, submitter2, bitstreamReadGroup));

       getClient(tokenAdminCol).perform(delete("/api/eperson/groups/"
                                         + bitstreamReadGroup.getID() + "/epersons/" + submitter1.getID()))
                                .andExpect(status().isNoContent());

      assertFalse(groupService.isMember(context, submitter1, bitstreamReadGroup));

    }

    @Test
    public void commAdminAndColAdminCanManageWorkflowGroupsTest() throws Exception {
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        context.turnOffAuthorisationSystem();

        EPerson adminChild1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Oliver", "Rossi")
                .withEmail("adminChild1@example.com")
                .withPassword(password)
                .build();
        EPerson adminCol1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("James", "Rossi")
                .withEmail("adminCol1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter1 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Carl", "Rossi")
                .withEmail("submitter1@example.com")
                .withPassword(password)
                .build();
        EPerson submitter2 = EPersonBuilder.createEPerson(context)
                .withNameInMetadata("Robert", "Clarks")
                .withEmail("submitter2@example.com")
                .withPassword(password)
                .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .withAdminGroup(adminChild1)
                                           .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withAdminGroup(adminCol1)
                                           .withWorkflowGroup(1, eperson)
                                           .withWorkflowGroup(2, eperson)
                                           .build();

        Group workflowGroupStep1 = col1.getWorkflowStep1(context);
        Group workflowGroupStep2 = col1.getWorkflowStep2(context);

        context.restoreAuthSystemState();

        assertFalse(groupService.isMember(context, submitter1, workflowGroupStep1));
        assertFalse(groupService.isMember(context, submitter2, workflowGroupStep2));

        String tokenAdminComm = getAuthToken(adminChild1.getEmail(), password);
        String tokenAdminCol = getAuthToken(adminChild1.getEmail(), password);

        getClient(tokenAdminComm).perform(post("/api/eperson/groups/" + workflowGroupStep1.getID() + "/epersons")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(REST_SERVER_URL + "eperson/groups/" + submitter1.getID()))
                .andExpect(status().isNoContent());

       assertTrue(groupService.isMember(context, submitter1, workflowGroupStep1));

       getClient(tokenAdminCol).perform(post("/api/eperson/groups/" + workflowGroupStep2.getID() + "/epersons")
               .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
               .content(REST_SERVER_URL + "eperson/groups/" + submitter2.getID()))
               .andExpect(status().isNoContent());

      assertTrue(groupService.isMember(context, submitter2, workflowGroupStep2));

      getClient(tokenAdminComm).perform(delete("/api/eperson/groups/"
                                        + workflowGroupStep2.getID() + "/epersons/" + submitter2.getID()))
                               .andExpect(status().isNoContent());

      getClient(tokenAdminCol).perform(delete("/api/eperson/groups/"
                                        + workflowGroupStep1.getID() + "/epersons/" + submitter1.getID()))
                               .andExpect(status().isNoContent());

      assertFalse(groupService.isMember(context, submitter1, workflowGroupStep1));
      assertFalse(groupService.isMember(context, submitter2, workflowGroupStep2));
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
                .contentType(parseMediaType
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
                .contentType(parseMediaType
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
                .contentType(parseMediaType
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
                .contentType(parseMediaType
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
                .contentType(parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/epersons/" + ePerson.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));

        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/epersons/" + ePerson.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/epersons"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.epersons", Matchers.hasItem(
                            EPersonMatcher.matchEPersonOnEmail(ePerson.getEmail())
                        )));
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
                .contentType(parseMediaType
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
                .contentType(parseMediaType
                    (org.springframework.data.rest.webmvc.RestMediaTypes
                         .TEXT_URI_LIST_VALUE))
                .content("https://localhost:8080/server/api/eperson/groups/" + group.getID()));

        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));

        configurationService.setProperty("core.authorization.community-admin.collection.admin-group", false);
        configurationService.setProperty("core.authorization.collection-admin.admin-group", false);

        getClient(token).perform(delete("/api/eperson/groups/" + adminGroup.getID() + "/subgroups/" + group.getID()))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID() + "/subgroups"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.subgroups", Matchers.hasItem(
                            GroupMatcher.matchGroupWithName(group.getName())
                        )));
    }

}
