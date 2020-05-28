/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.allOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityAdminGroupRestControllerIT extends AbstractControllerIntegrationTest {


    @Autowired
    private CommunityService communityService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void getCommunityAdminGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCommunityAdminGroupTestCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }


    @Test
    public void getCommunityAdminGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCommunityAdminGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getCommunityAdminGroupNoContentTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void getCommunityAdminGroupWrongCommunityUuidResourceNotFoundTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        // no needs to explicitly cleanup the group created as the community comes
        // from a CommunityBuilder that will cleanup also related groups
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupExtraMetadataSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        // no needs to explicitly cleanup the group created as the community comes
        // from a CommunityBuilder that will cleanup also related groups
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", allOf(
                            matchMetadata("dc.description", "testingDescription"),
                            matchMetadata("dc.subject", "testSubject"))))
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupDcTitleUnprocessable() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }


    @Test
    public void postCommunityAdminGroupCreateAdminGroupSuccessCommunityAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        // no needs to explicitly cleanup the group created as the community comes
        // from a CommunityBuilder that will cleanup also related groups
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());


        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupNotFound() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/communities/" + UUID.randomUUID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupUnProcessableName() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());


        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void postCommunityAdminGroupCreateAdminGroupUnProcessablePermanent() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/communities/" + parentCommunity.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void deleteCommunityAdminGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }


    @Test
    public void deleteCommunityAdminGroupTestCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Group adminGroup = communityService.createAdministrators(context, child1);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/communities/" + child1.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/communities/" + child1.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCommunityAdminGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$",
                                GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void deleteCommunityAdminGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$",
                                GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }


    @Test
    public void deleteCommunityAdminGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/communities/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());
    }
}
