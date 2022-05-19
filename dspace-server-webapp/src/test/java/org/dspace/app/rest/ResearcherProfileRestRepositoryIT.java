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
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.model.MetadataValueRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.ResearcherProfileRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
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
            .withTemplateItem()
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
            .withDspaceObjectOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.object.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
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
            .withDspaceObjectOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id.toString())))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.object.owner", name, id.toString(), 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
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
            .withDspaceObjectOwner(name, id.toString())
            .build();

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
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

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.object.owner", name, id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));
    }

    @Test
    public void testCreateAndReturnWithPublicProfile() throws Exception {

        configurationService.setProperty("researcher-profile.set-new-profile-visible", true);
        String id = user.getID().toString();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(true)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));
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

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("item")))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.object.owner", name, id, 0)))
            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("eperson")))
            .andExpect(jsonPath("$.name", is(name)));

        authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));
    }

    @Test
    public void testCreateAndReturnWithoutCollectionIdSet() throws Exception {

        String id = user.getID().toString();

        configurationService.setProperty("researcher-profile.collection.uuid", null);

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        String itemId = getItemIdByProfileId(authToken, id);
        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getOwningCollection(), is(personCollection));

    }

    @Test
    public void testCreateAndReturnWithCollectionHavingInvalidEntityTypeSet() throws Exception {

        String id = user.getID().toString();

        context.turnOffAuthorisationSystem();

        Collection orgUnitCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("OrgUnit Collection")
            .withEntityType("OrgUnit")
            .withSubmitterGroup(user)
            .withTemplateItem()
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("researcher-profile.collection.uuid", orgUnitCollection.getID().toString());

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + id, "item", "eperson")));

        String itemId = getItemIdByProfileId(authToken, id);
        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getOwningCollection(), is(personCollection));

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

        getClient(authToken).perform(post("/api/eperson/profiles/")
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

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(id)))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

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

        getClient(authToken).perform(post("/api/eperson/profiles/")
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
        AtomicReference<UUID> itemIdRef = new AtomicReference<>();

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("dspace.object.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(authToken).perform(get("/api/core/items/{id}", itemIdRef.get()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataDoesNotExist("dspace.object.owner"))));

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
        AtomicReference<UUID> itemIdRef = new AtomicReference<>();

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("dspace.object.owner"))))
            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(), "$.id"))));

        getClient(authToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(userToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(adminToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(adminToken).perform(post("/api/eperson/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(userToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNotFound());

        getClient(adminToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(userToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        getClient(anotherUserToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        String itemId = getItemIdByProfileId(authToken, id);

        getClient().perform(get("/api/core/items/{id}", itemId))
            .andExpect(status().isUnauthorized());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient().perform(get("/api/core/items/{id}", itemId))
            .andExpect(status().isOk());

        // change the visibility to false
        operations = asList(new ReplaceOperation("/visible", false));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient().perform(get("/api/core/items/{id}", itemId))
            .andExpect(status().isUnauthorized());

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

        getClient(userToken).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.visible", is(false)));

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        // try to change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(anotherUserToken).perform(patch("/api/eperson/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(userToken).perform(post("/api/eperson/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(adminToken).perform(patch("/api/eperson/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
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

        getClient(adminToken).perform(post("/api/eperson/profiles/")
            .param("eperson", id)
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(adminToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        // change the visibility to true
        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(userToken).perform(patch("/api/eperson/profiles/{id}", id)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.visible", is(true)));
    }

    @Test
    public void testPatchToChangeVisibleAttributeOfNotExistProfile() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$.visible", is(false)));

        getClient(authToken).perform(delete("/api/eperson/profiles/{id}", id))
                            .andExpect(status().isNoContent());

        List<Operation> operations = asList(new ReplaceOperation("/visible", true));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", id)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isNotFound());
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
        getClient(adminToken).perform(post("/api/eperson/profiles/")
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

        getClient(adminToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        getClient(adminToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNotFound());

        // the automatic claim is done after the user login
        String userToken = getAuthToken(user.getEmail(), password);

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        // the profile item should be the same
        String secondItemId = getItemIdByProfileId(adminToken, id);
        assertEquals("The item should be the same", firstItemId, secondItemId);

    }

    @Test
    public void testAutomaticProfileClaimByEmailWithRegularEntity() throws Exception {

        String userToken = getAuthToken(user.getEmail(), password);

        context.turnOffAuthorisationSystem();

        Item itemToBeClaimed = ItemBuilder.createItem(context, personCollection)
            .withPersonEmail(user.getEmail())
            .build();

        context.restoreAuthSystemState();

        String id = user.getID().toString();

        getClient(userToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNotFound());

        // the automatic claim is done after the user login
        String newUserToken = getAuthToken(user.getEmail(), password);

        getClient(newUserToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        // the profile item should be the same
        String firstItemId = itemToBeClaimed.getID().toString();
        String secondItemId = getItemIdByProfileId(newUserToken, id);
        assertEquals("The item should be the same", firstItemId, secondItemId);

    }

    @Test
    public void testNoAutomaticProfileClaimOccursIfManyClaimableItemsAreFound() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withPersonEmail("test@email.it")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withPersonEmail("test@email.it")
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(get("/api/eperson/profiles/{id}", epersonId))
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
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        getClient(token).perform(get("/api/eperson/profiles/{id}", epersonId))
            .andExpect(status().isOk());

        String profileItemId = getItemIdByProfileId(token, epersonId);

        context.turnOffAuthorisationSystem();

        ItemBuilder.createItem(context, personCollection)
            .withPersonEmail("test@email.it")
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
            .withDspaceObjectOwner("Admin User", admin.getID().toString())
            .build();

        context.restoreAuthSystemState();

        String epersonId = ePerson.getID().toString();

        String token = getAuthToken(ePerson.getEmail(), password);

        getClient(token).perform(get("/api/eperson/profiles/{id}", epersonId))
            .andExpect(status().isNotFound());

    }

    @Test
    public void researcherProfileClaim() throws Exception {
        String id = user.getID().toString();
        String name = user.getName();

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
                                      .withTitle("Test User 1")
                                      .withPersonEmail(user.getEmail())
                                      .build();

        final Item otherPerson = ItemBuilder.createItem(context, personCollection)
                                       .withTitle("Test User 2")
                                       .withPersonEmail(user.getEmail())
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$.id", is(id)))
                            .andExpect(jsonPath("$.type", is("profile")))
                            .andExpect(jsonPath("$", matchLinks("http://localhost/api/eperson/profiles/" + user.getID(),
                                                                "item", "eperson")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
                            .andExpect(status().isOk());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.type", is("item")))
                            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.object.owner", name, id, 0)))
                            .andExpect(jsonPath("$.metadata", matchMetadata("dspace.entity.type", "Person", 0)));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/eperson", id))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.type", is("eperson")))
                            .andExpect(jsonPath("$.name", is(name)));

        // trying to claim another profile
        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + otherPerson.getID().toString()))
                            .andExpect(status().isUnprocessableEntity());

        // other person trying to claim same profile
        context.turnOffAuthorisationSystem();
        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withEmail("foo@bar.baz")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        context.restoreAuthSystemState();

        final String ePersonToken = getAuthToken(ePerson.getEmail(), password);

        getClient(ePersonToken).perform(post("/api/eperson/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isBadRequest());

        getClient(authToken).perform(delete("/api/eperson/profiles/{id}", id))
                            .andExpect(status().isNoContent());
    }

    @Test
    public void researcherProfileClaimWithDifferentEmail() throws Exception {

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User 1")
            .withPersonEmail(eperson.getEmail())
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(TEXT_URI_LIST)
            .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testNotAdminUserClaimProfileOfAnotherUser() throws Exception {

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
                                       .withTitle("Test User 1")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .param("eperson" , anotherUser.getID().toString())
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void testAdminUserClaimProfileOfNotExistingPersonId() throws Exception {

        String id = "bef23ba3-9aeb-4f7b-b153-77b0f1fc3612";

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
                                       .withTitle("Test User 1")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .param("eperson" , id)
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void testAdminUserClaimProfileOfWrongPersonId() throws Exception {

        String id = "invalid_id";

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
                                       .withTitle("Test User 1")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .param("eperson" , id)
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
                            .andExpect(status().isBadRequest());
    }

    @Test
    public void claimForNotAllowedEntityType() throws Exception {
        context.turnOffAuthorisationSystem();

        final Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
                                                        .withEntityType("Publication")
                                                        .build();

        final Item publication = ItemBuilder.createItem(context, publications)
                                       .withTitle("title")
                                       .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(TEXT_URI_LIST)
                                         .content("http://localhost:8080/server/api/core/items/" + publication.getID().toString()))
                            .andExpect(status().isBadRequest());
    }

    @Test
    public void testCloneFromExternalProfileAlreadyAssociated() throws Exception {

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/").contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated()).andExpect(jsonPath("$.id", is(id)))
                            .andExpect(jsonPath("$.visible", is(false))).andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken)
            .perform(post("/api/eperson/profiles/").contentType(TEXT_URI_LIST)
                                                .content("http://localhost:8080/server/api/core/items/" + id))
            .andExpect(status().isUnprocessableEntity());
    }

    private String getItemIdByProfileId(String token, String id) throws Exception {
        MvcResult result = getClient(token).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }
}
