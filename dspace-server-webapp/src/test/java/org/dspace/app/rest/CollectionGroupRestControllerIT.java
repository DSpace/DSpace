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
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.model.MetadataRest;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
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
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
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
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
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
        getClient(token).perform(get("/api/eperson/groups/" + adminGroup.getID()))
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
}
