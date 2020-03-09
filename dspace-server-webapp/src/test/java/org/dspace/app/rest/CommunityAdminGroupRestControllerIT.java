/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
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
import org.dspace.content.service.CommunityService;
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

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
    }

    @Test
    public void getCommunityAdminGroupTest() throws Exception {
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCommunityAdminGroupUnAuthorizedTest() throws Exception {
        communityService.createAdministrators(context, parentCommunity);

        getClient().perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCommunityAdminGroupForbiddenTest() throws Exception {
        communityService.createAdministrators(context, parentCommunity);

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

    }

    @Test
    public void deleteCommunityAdminGroupTest() throws Exception {
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);
        
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCommunityAdminGroupUnAuthorizedTest() throws Exception {
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);


        getClient().perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                        .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCommunityAdminGroupForbiddenTest() throws Exception {
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/communities/" + parentCommunity.getID() + "/adminGroup"))
                   .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCommunityAdminGroupNotFoundTest() throws Exception {
        Group adminGroup = communityService.createAdministrators(context, parentCommunity);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/communities/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());
    }
}
