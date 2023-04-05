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
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowService;
import org.junit.Before;
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

        context.restoreAuthSystemState();
    }

    @Test
    public void getCollectionAdminGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void getCollectionAdminGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionAdminGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

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
    public void postCollectionAdminGroupCreateAdminGroupExtraMetadataSuccess() throws Exception {

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
        Group adminGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", allOf(
                            matchMetadata("dc.description", "testingDescription"),
                            matchMetadata("dc.subject", "testSubject"))))
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));

    }

    @Test
    public void postCollectionAdminGroupCreateAdminGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                   .andExpect(status().isNoContent());

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

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void deleteCollectionAdminGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }


    @Test
    public void deleteCollectionAdminGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionAdminGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isForbidden());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk());
    }

    @Test
    public void deleteCollectionAdminGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                    .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                    .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }

    @Test
    public void deleteCollectionAdminGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }


    @Test
    public void deleteCollectionAdminGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group adminGroup = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/adminGroup"))
                        .andExpect(status().isNotFound());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/adminGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(adminGroup.getID(), adminGroup.getName())));
    }


    @Test
    public void getCollectionSubmittersGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submitters = collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submitters = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submitters = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submitters.getID(), submitters.getName())));
    }

    @Test
    public void getCollectionSubmittersGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionSubmittersGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

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
    public void postCollectionSubmitterGroupCreateSubmitterGroupExtraMetadataSuccess() throws Exception {

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
        Group submittersGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", allOf(
                            matchMetadata("dc.description", "testingDescription"),
                            matchMetadata("dc.subject", "testSubject"))))
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(submittersGroup.getID(), submittersGroup.getName())));

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/submittersGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                   .andExpect(status().isNoContent());

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

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionSubmitterGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }


    @Test
    public void deleteCollectionSubmittersGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionSubmittersGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionSubmittersGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submittersGroup.getID(),
                                                                       submittersGroup.getName())));

    }

    @Test
    public void deleteCollectionSubmittersGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/submittersGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(submittersGroup.getID(),
                                                                       submittersGroup.getName())));
    }


    @Test
    public void deleteCollectionSubmittersGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group submittersGroup = collectionService.createSubmitters(context, collection);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/submittersGroup"))
                        .andExpect(status().isNotFound());
    }


    @Test
    public void getCollectionItemReadGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultItemReadGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionDefaultItemReadGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/itemReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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

        String token = getAuthToken(admin.getEmail(), password);
        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));


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

        token = getAuthToken(admin.getEmail(), password);
        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));


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

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));


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


        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

    }

    @Test
    public void deleteCollectionDefaultItemReadGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that deleting a Default Read Group has the expected behaviour of defaulting the DefaultReadGroup back
        // to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

    }


    @Test
    public void deleteCollectionDefaultItemReadGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void deleteCollectionDefaultItemReadGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void deleteCollectionDefaultItemReadGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void deleteCollectionDefaultItemReadGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/itemReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }


    @Test
    public void deleteCollectionDefaultItemReadGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String itemGroupString = "ITEM";
        int defaultItemRead = Constants.DEFAULT_ITEM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, itemGroupString, defaultItemRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/itemReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getCollectionBitstreamReadGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getCollectionDefaultBitstreamReadGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());
        Group anon = groupService.findByName(context, Group.ANONYMOUS);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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

        String token = getAuthToken(admin.getEmail(), password);
        Group anon = groupService.findByName(context, Group.ANONYMOUS);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));


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


        token = getAuthToken(admin.getEmail(), password);
        Group anon = groupService.findByName(context, Group.ANONYMOUS);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

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


        Group anon = groupService.findByName(context, Group.ANONYMOUS);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

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


        Group anon = groupService.findByName(context, Group.ANONYMOUS);

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that deleting a Default Read Group has the expected behaviour of defaulting the DefaultReadGroup back
        // to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }


    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));

    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNoContent());

        // Note that the Default Read Group has the expected behaviour of defaulting to the Anonymous group
        Group anon = groupService.findByName(context, Group.ANONYMOUS);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(anon.getID(), anon.getName())));
    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                   .andExpect(status().isOk())
                   .andExpect(
                       jsonPath("$", GroupMatcher.matchGroupEntry(role.getID(), role.getName())));
    }

    @Test
    public void deleteCollectionDefaultBitstreamReadGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/bitstreamReadGroup"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void deleteCollectionDefaultBitstreamReadGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        String bitstreamGroupString = "BITSTREAM";
        int defaultBitstreamRead = Constants.DEFAULT_BITSTREAM_READ;

        Group role = collectionService.createDefaultReadGroup(context, collection, bitstreamGroupString,
                                                              defaultBitstreamRead);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/bitstreamReadGroup"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void getWorkflowGroupForCollectionAndRole() throws Exception {

        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupForCollectionAndRoleParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupForCollectionAndRoleWrongUUIDCollectionNotFound() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

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

        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$", GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void getWorkflowGroupUnAuthorized() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void getWorkflowGroupForbidden() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isForbidden());
    }


    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupSuccess() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        GroupRest groupRest = new GroupRest();
        MetadataRest metadataRest = new MetadataRest();

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
        Group workflowGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(
                            jsonPath("$",
                                     GroupMatcher.matchGroupEntry(workflowGroup.getID(), workflowGroup.getName())));

    }

    @Test
    public void postCollectionWorkflowGroupCreateWorkflowGroupExtraMetadataSuccess() throws Exception {

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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
        Group workflowGroup = groupService.find(context, idRef.get());
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.metadata", allOf(
                            matchMetadata("dc.description", "testingDescription"),
                            matchMetadata("dc.subject", "testSubject"))))
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


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(post("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer")
                                     .content(mapper.writeValueAsBytes(groupRest))
                                     .contentType(contentType))
                        .andExpect(status().isUnprocessableEntity());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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
        // no needs to explicitly cleanup the group created as the collection comes
        // from a CollectionBuilder that will cleanup also related groups
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


        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                   .andExpect(status().isNoContent());

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

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

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

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

    }

    @Test
    public void deleteCollectionWorkflowGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
    }


    @Test
    public void deleteCollectionWorkflowGroupTestParentCommunityAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, parentCommunity, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionWorkflowGroupTestCollectionAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        authorizeService.addPolicy(context, collection, Constants.ADMIN, eperson);
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());

        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void deleteCollectionWorkflowGroupUnAuthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                   .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$",
                                     GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }

    @Test
    public void deleteCollectionWorkflowGroupForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isForbidden());

        token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$",
                                            GroupMatcher.matchGroupEntry(group.getID(), group.getName())));
    }


    @Test
    public void deleteCollectionWorkflowGroupNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group group = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + UUID.randomUUID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isNotFound());
    }

    @Test
    public void deleteCollectionWorkflowGroupWithPooledTaskTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Group reviewer = workflowService.createWorkflowRoleGroup(context, collection, "reviewer");

        // Submit an Item into the workflow -> moves to the "reviewer" step's pool.
        // The role must have at least one EPerson, otherwise the WSI gets archived immediately
        groupService.addMember(context, reviewer, eperson);
        workflowService.start(
            context,
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                                .withTitle("Dummy Item")
                                .build()
        );

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(delete("/api/core/collections/" + collection.getID() + "/workflowGroups/reviewer"))
                        .andExpect(status().isUnprocessableEntity());
    }

}
