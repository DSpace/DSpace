/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.profile.OrcidEntitySynchronizationPreference.ALL;
import static org.dspace.app.profile.OrcidEntitySynchronizationPreference.MINE;
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link ResearcherProfileRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    private EPerson user;

    private EPerson anotherUser;

    private Collection personCollection;

    /**
     * Tests setup.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        user = EPersonBuilder.createEPerson(context)
            .withEmail("user@example.com")
            .withPassword(password)
            .build();

        anotherUser = EPersonBuilder.createEPerson(context)
            .withEmail("anotherUser@example.com")
            .withPassword(password)
            .build();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        personCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Profile Collection")
            .withEntityType("Person")
            .withSubmitterGroup(user)
            .build();

        configurationService.setProperty("researcher-profile.collection.uuid", personCollection.getID().toString());

        context.setCurrentUser(user);

        context.restoreAuthSystemState();

    }

    /**
     * Verify that the findById endpoint returns the own profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindById() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(user.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist())
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that the an admin user can call the findById endpoint to get a
     * profile.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithAdmin() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(admin.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

    }

    /**
     * Verify that a standard user can't access the profile of another user.
     *
     * @throws Exception
     */
    @Test
    public void testFindByIdWithoutOwnerUser() throws Exception {

        UUID id = user.getID();
        String name = user.getFullName();

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withCrisOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isForbidden());

    }

    /**
     * Verify that the createAndReturn endpoint create a new researcher profile.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturn() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));
    }

    /**
     * Verify that an admin can call the createAndReturn endpoint to store a new
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithAdmin() throws Exception {

        String id = user.getID().toString();
        String name = user.getName();

        configurationService.setProperty("researcher-profile.collection.uuid", null);

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("cris.sourceId", id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

        authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/cris/profiles/" + id, "item", "eperson")));
    }

    /**
     * Verify that a standard user can't call the createAndReturn endpoint to store
     * a new researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithoutOwnUser() throws Exception {

        String authToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", user.getID().toString())
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

    }

    /**
     * Verify that a conflict occurs if an user that have already a profile call the
     * createAndReturn endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

    }

    /**
     * Verify that an unprocessable entity status is back when the createAndReturn
     * is called to create a profile for an unknown user.
     *
     * @throws Exception
     */
    @Test
    public void testCreateAndReturnWithUnknownEPerson() throws Exception {

        String unknownId = UUID.randomUUID().toString();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .param("eperson", unknownId)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Verify that a user can delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataDoesNotExist("cris.owner"))));

    }

    /**
     * Verify that a user can hard delete his profile using the delete endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testHardDelete() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", true);

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("cris.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get()))
            .andExpect(status().isNotFound());

    }

    /**
     * Verify that an admin can delete a profile of another user using the delete
     * endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());
    }

    /**
     * Verify that an user can delete his profile using the delete endpoint even if
     * was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(userToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

    }

    /**
     * Verify that a standard user can't call the delete endpoint to delete a
     * researcher profile related to another user.
     *
     * @throws Exception
     */
    @Test
    public void testDeleteWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(anotherUserToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

    }

    /**
     * Verify that an user can change the profile visibility using the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttribute() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        // change the visibility to false
        operations = asList(new ReplaceOperation("/visible", false));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

    }

    /**
     * Verify that an user can not change the profile visibility of another user
     * using the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithoutOwnUser() throws Exception {

        String id = user.getID().toString();

        String userToken = getAuthToken(user.getEmail(), password);
        String anotherUserToken = getAuthToken(anotherUser.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // try to change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(anotherUserToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));
    }

    /**
     * Verify that an admin can change the profile visibility of another user using
     * the patch endpoint.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibleAttributeWithAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(adminToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that an user can change the visibility of his profile using the patch
     * endpoint even if was created by an admin.
     *
     * @throws Exception
     */
    @Test
    public void testPatchToChangeVisibilityOfProfileCreatedByAnAdmin() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(userToken).perform(patch("/api/cris/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));
    }

    /**
     * Verify that after an user login an automatic claim between the logged eperson
     * and possible profiles without eperson is done.
     *
     * @throws Exception
     */
    @Test
    public void testAutomaticProfileClaimByEmail() throws Exception {

        String id = user.getID().toString();

        String adminToken = getAuthToken(admin.getEmail(), password);

        // create and delete a profile
        getClient(adminToken).perform(post("/api/cris/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        String firstItemId = getItemIdByProfileId(adminToken, id);

        MetadataValueRest valueToAdd = new MetadataValueRest(user.getEmail());
        List<Operation> operations = asList(new AddOperation("/metadata/person.email", valueToAdd));

        getClient(adminToken).perform(patch(BASE_REST_SERVER_URL + "/api/core/items/{id}", firstItemId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getPatchContent(operations)))
            .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/cris/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isNotFound());

        // the automatic claim is done after the user login
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(get("/api/cris/profiles/{id}", id))
            .andExpect(status().isOk());

        // the profile item should be the same
        String secondItemId = getItemIdByProfileId(adminToken, id);
        assertEquals("The item should be the same", firstItemId, secondItemId);

    }

    /**
     * Verify that after an user login an automatic claim between the logged eperson
     * and possible profiles without eperson is done.
     *
     * @throws Exception
     */
    @Test
    public void testAutomaticProfileClaimByOrcid() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .withOrcid("0000-1111-2222-3333")
            .build();

        Item item = ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId))
            .andExpect(status().isOk());

        String profileItemId = getItemIdByProfileId(token, epersonId);
        assertEquals("The item should be the same", item.getID().toString(), profileItemId);

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfManyClaimableItemsAreFound() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .withOrcid("0000-1111-2222-3333")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User 2")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(get("/api/cris/profiles/{id}", epersonId))
            .andExpect(status().isNotFound());

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfTheUserHasAlreadyAProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .withOrcid("0000-1111-2222-3333")
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId))
            .andExpect(status().isOk());

        String profileItemId = getItemIdByProfileId(token, epersonId);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User")
            .withOrcidIdentifier("0000-1111-2222-3333")
            .build();

        context.restoreAuthSystemState();

        token = getAuthToken(ePerson.getEmail(), password);

        String newProfileItemId = getItemIdByProfileId(token, epersonId);
        assertEquals("The item should be the same", newProfileItemId, profileItemId);

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfTheFoundProfileIsAlreadyClaimed() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withTitle("Admin User")
            .withPersonEmail("test@email.it")
            .withCrisOwner("Admin User", admin.getID().toString())
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(get("/api/cris/profiles/{id}", epersonId))
            .andExpect(status().isNotFound());

    }

    @Test
    public void testOrcidMetadataOfEpersonAreCopiedOnProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(ePersonId.toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcid", is("0000-1111-2222-3333")))
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")))
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is("DISABLED")))
            .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is("DISABLED")))
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences", empty()));

        String itemId = getItemIdByProfileId(authToken, ePersonId);

        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));
        assertThat(profileItem, notNullValue());

        List<MetadataValue> metadata = profileItem.getMetadata();
        assertThat(metadata, hasItem(with("person.identifier.orcid", "0000-1111-2222-3333")));
        assertThat(metadata, hasItem(with("cris.orcid.access-token", "af097328-ac1c-4a3e-9eb4-069897874910")));
        assertThat(metadata, hasItem(with("cris.orcid.refresh-token", "32aadae0-829e-49c5-824f-ccaf4d1913e4")));
        assertThat(metadata, hasItem(with("cris.orcid.scope", "/first-scope", 0)));
        assertThat(metadata, hasItem(with("cris.orcid.scope", "/second-scope", 1)));

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForPublications() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/publications", ALL.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/publications", MINE.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(MINE.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(MINE.name())));

        operations = asList(new ReplaceOperation("/orcid/publications", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForProjects() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/projects", ALL.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/projects", MINE.name()));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is(MINE.name())));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.projectsPreference", is(MINE.name())));

        operations = asList(new ReplaceOperation("/orcid/projects", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/profile", "AFFILIATION, EDUCATION"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                containsInAnyOrder("AFFILIATION", "EDUCATION")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                containsInAnyOrder("AFFILIATION", "EDUCATION")));

        operations = asList(new ReplaceOperation("/orcid/profile", "IDENTIFIERS"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                containsInAnyOrder("IDENTIFIERS")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                containsInAnyOrder("IDENTIFIERS")));

        operations = asList(new ReplaceOperation("/orcid/profiles", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationMode() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        operations = asList(new ReplaceOperation("/orcid/mode", "MANUAL"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        getClient(authToken).perform(get("/api/cris/profiles/{id}", ePersonId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        operations = asList(new ReplaceOperation("/orcid/mode", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithWrongPath() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidAccessToken("af097328-ac1c-4a3e-9eb4-069897874910")
            .withOrcidRefreshToken("32aadae0-829e-49c5-824f-ccaf4d1913e4")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/wrong-path", "BATCH"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithProfileNotLinkedToOrcid() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/cris/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken).perform(patch("/api/cris/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest());
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/cris/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }
}
