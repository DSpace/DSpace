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

import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CollectionGroupRestControllerIT extends AbstractControllerIntegrationTest {


    @Autowired
    private CollectionService collectionService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private WorkflowService workflowService;

    Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("test").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

    }

    @Test
    public void getCollectionAdminGroupTest() throws Exception {

        Group adminGroup = collectionService.createAdministrators(context, collection);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupTestParentCommunityAdmin() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupTestCollectionAdmin() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupUnAuthorizedTest() throws Exception {
        collectionService.createAdministrators(context, collection);

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionAdminGroupForbiddenTest() throws Exception {
        collectionService.createAdministrators(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionAdminGroupNoContentTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void getCollectionAdminGroupWrongCollectionUuidResourceNotFoundTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupDcTitleUnprocessable() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void postCollectionAdminGroupCreateAdminGroupSuccessParentCommunityAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupSuccessCollectionAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupNotFound() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + UUID.randomUUID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupUnProcessableName() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupUnProcessablePermanent() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/adminGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void deleteCollectionAdminGroupTest() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }


    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionAdminGroupTestParentCommunityAdmin() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionAdminGroupTestCollectionAdmin() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionAdminGroupUnAuthorizedTest() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);


        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCollectionAdminGroupForbiddenTest() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionAdminGroupNotFoundTest() throws Exception {
        Group adminGroup = collectionService.createAdministrators(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());
    }


    @Test
    public void getCollectionSubmittersGroupTest() throws Exception {

        Group submitters = collectionService.createSubmitters(context, collection);

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupTestParentCommunityAdmin() throws Exception {

        Group submitters = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupTestCollectionAdmin() throws Exception {

        Group submitters = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupUnAuthorizedTest() throws Exception {
        collectionService.createSubmitters(context, collection);

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionSubmittersGroupForbiddenTest() throws Exception {
        collectionService.createSubmitters(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionSubmittersGroupNoContentTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void getCollectionSubmittersGroupWrongCollectionUuidResourceNotFoundTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/submittersGroup"))
                        .andExpect(status().isNotFound());
    }


    @Test
    public void postCollectionSubmitterGroupCreateSubmitterGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group submittersGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(submittersGroup.getID(), submittersGroup.getName())));

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupDcTitleUnprocessable() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupSuccessParentCommunityAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group submittersGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(submittersGroup.getID(), submittersGroup.getName())));

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupSuccessCollectionAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group submittersGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(submittersGroup.getID(), submittersGroup.getName())));

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupNotFound() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + UUID.randomUUID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupUnProcessableName() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postCollectionSubmittersGroupCreateSubmittersGroupUnProcessablePermanent() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void deleteCollectionSubmitterGroupTest() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }


    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionSubmittersGroupTestParentCommunityAdmin() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionSubmittersGroupTestCollectionAdmin() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionSubmittersGroupUnAuthorizedTest() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);


        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCollectionSubmittersGroupForbiddenTest() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionSubmittersGroupNotFoundTest() throws Exception {
        Group submittersGroup = collectionService.createSubmitters(context, collection);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/submittersGroup"))
                        .andExpect(status().isNotFound());
    }

    private Group createGroup(String itemGroupString, int defaultItemRead) throws SQLException, AuthorizeException {
        Group role = groupService.create(context);
        groupService.setName(role, "COLLECTION_" + collection.getID().toString() + "_" + itemGroupString +
            "_DEFAULT_READ");

        // Remove existing privileges from the anonymous group.
        authorizeService.removePoliciesActionFilter(context, collection, defaultItemRead);

        // Grant our new role the default privileges.
        authorizeService.addPolicy(context, collection, defaultItemRead, role);
        groupService.update(context, role);
        return role;
    }

    @Test
    public void getCollectionItemReadGroupTest() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupTestParentCommunityAdmin() throws Exception {
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupTestCollectionAdmin() throws Exception {
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupUnAuthorizedTest() throws Exception {
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionDefaultItemReadGroupForbiddenTest() throws Exception {
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionDefaultItemReadGroupAnonymousGroupTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupWrongCollectionUuidResourceNotFoundTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/itemReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group itemReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(itemReadGroup.getID(), itemReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupDcTitleUnprocessable() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupSuccessParentCommunityAdmin()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group itemReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(itemReadGroup.getID(), itemReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupSuccessCollectionAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group itemReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(itemReadGroup.getID(), itemReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupNotFound() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + UUID.randomUUID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupUnProcessableName() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postCollectionDefaultItemReadGroupCreateDefaultItemReadGroupUnProcessablePermanent() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void deleteCollectionDefaultItemReadGroupTest() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

    }


    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionDefaultItemReadGroupTestParentCommunityAdmin() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());
    }

    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionDefaultItemReadGroupTestCollectionAdmin() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionDefaultItemReadGroupUnAuthorizedTest() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCollectionDefaultItemReadGroupForbiddenTest() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionDefaultItemReadGroupNotFoundTest() throws Exception {

        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = createGroup(itemGroupString, defaultItemRead);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/itemReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getCollectionBitstreamReadGroupTest() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupTestParentCommunityAdmin() throws Exception {
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupTestCollectionAdmin() throws Exception {
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupUnAuthorizedTest() throws Exception {
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupForbiddenTest() throws Exception {
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupNoContentTest() throws Exception {

        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupWrongCollectionUuidResourceNotFoundTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group bitstreamReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher
                                .matchGroupEntry(bitstreamReadGroup.getID(), bitstreamReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupDcTitleUnprocessable()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupSuccessParentCommunityAdmin()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group bitstreamReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher
                                .matchGroupEntry(bitstreamReadGroup.getID(), bitstreamReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupSuccessCollectionAdmin()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group bitstreamReadGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher
                                .matchGroupEntry(bitstreamReadGroup.getID(), bitstreamReadGroup.getName())));

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupNotFound() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + UUID.randomUUID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupUnProcessableName()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postCollectionDefaultBitstreamReadGroupCreateDefaultBitstreamReadGroupUnProcessablePermanent()
        throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTest() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }


    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTestParentCommunityAdmin() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());
    }

    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTestCollectionAdmin() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupUnAuthorizedTest() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupForbiddenTest() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionDefaultBitstreamReadGroupNotFoundTest() throws Exception {

        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = createGroup(bitstreamGroupString, defaultBitstreamRead);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowGroupForCollectionAndRole() throws Exception {


        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupForCollectionAndRoleParentCommunityAdmin() throws Exception {
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupForCollectionAndRoleWrongUUIDCollectionNotFound() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowGroupForCollectionAndRoleWrongRoleNotFound() throws Exception {


        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/workflowGroups/wrongRole"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowGroupCommunityAdmin() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupCollectionAdmin() throws Exception {
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupUnAuthorized() throws Exception {
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowGroupForbidden() throws Exception {
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group workflowGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(workflowGroup.getID(), workflowGroup.getName())));

    }

    @Test
    public void postCollectionWorkflowGroupWrongCollectionId() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + UUID.randomUUID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupWrongRole() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/wrongRole")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isNotFound());

    }


    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupDcTitleUnprocessable() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));
        metadataRest.put("dc.title", new MetadataValueRest("testTitle"));

        groupRest.setMetadata(metadataRest);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupSuccessParentCommunityAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group workflowGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(workflowGroup.getID(), workflowGroup.getName())));

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupSuccessCollectionAdmin() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        AtomicReference<UUID> idRef = new AtomicReference<>();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isCreated())
                        .andDo(result -> idRef
                            .set(UUID.fromString(read(result.getResponse().getContentAsString(), "$.id")))
                        );
        Group workflowGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(workflowGroup.getID(), workflowGroup.getName())));

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupUnAuthorized() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        getClient().perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                .content(mapper.writeValueAsBytes(groupRest))
                                .contentType(contentType))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupForbidden() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupUnProcessableName() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setName("Fail");
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupUnProcessablePermanent() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        groupRest.setPermanent(true);
        MetadataRest metadataRest = new MetadataRest();
        metadataRest.put("dc.description", new MetadataValueRest("testingDescription"));
        metadataRest.put("dc.subject", new MetadataValueRest("testSubject"));

        groupRest.setMetadata(metadataRest);

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void deleteCollectionWorkflowGroupTest() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNotFound());
    }


    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionWorkflowGroupTestParentCommunityAdmin() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
    }

    // This is currently not supported in DSpace API
    @Ignore
    @Test
    public void deleteCollectionWorkflowGroupTestCollectionAdmin() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionWorkflowGroupUnAuthorizedTest() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");


        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteCollectionWorkflowGroupForbiddenTest() throws Exception {

        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");


        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionWorkflowGroupNotFoundTest() throws Exception {
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");


        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNotFound());
    }

}
