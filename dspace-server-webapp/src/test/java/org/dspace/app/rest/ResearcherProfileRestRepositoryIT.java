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
import static org.dspace.app.matcher.LambdaMatcher.has;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.rest.matcher.HalMatcher.matchLinks;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataNotEmpty;
import static org.dspace.app.rest.matcher.ResourcePolicyMatcher.matchResourcePolicyProperties;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.profile.OrcidEntitySyncPreference.ALL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import com.jayway.jsonpath.JsonPath;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.ResearcherProfileRestRepository;
import org.dspace.app.rest.repository.patch.operation.ResearcherProfileAddOrcidOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.OrcidTokenBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.orcid.OrcidQueue;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.junit.After;
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

    @Autowired
    private OrcidTokenService orcidTokenService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Autowired
    private ResearcherProfileAddOrcidOperation researcherProfileAddOrcidOperation;

    @Autowired
    private OrcidClient orcidClient;

    private OrcidClient orcidClientMock = mock(OrcidClient.class);

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

        researcherProfileAddOrcidOperation.setOrcidClient(orcidClientMock);

    }

    @After
    public void after() {
        orcidTokenService.deleteAll(context);
        researcherProfileAddOrcidOperation.setOrcidClient(orcidClient);
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

        String itemId = getItemIdByProfileId(authToken, id);
        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/authz/resourcepolicies/search/resource")
                .param("uuid", itemId))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies", containsInAnyOrder(
                matchResourcePolicyProperties(null, user, profileItem, null, Constants.READ, null),
                matchResourcePolicyProperties(null, user, profileItem, null, Constants.WRITE, null))))
            .andExpect(jsonPath("$.page.totalElements", is(2)));

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

    @Test
    public void testDeleteWithProfileLinkedWithOrcid() throws Exception {

        configurationService.setProperty("researcher-profile.hard-delete.enabled", false);

        context.turnOffAuthorisationSystem();

        Item profileItem = ItemBuilder.createItem(context, personCollection)
            .withDspaceObjectOwner(user.getEmail(), user.getID().toString())
            .withOrcidIdentifier("0000-1111-2222-3333")
            .withOrcidAccessToken("access-token", eperson)
            .withOrcidAuthenticated("authenticated")
            .build();

        String id = user.getID().toString();
        String authToken = getAuthToken(user.getEmail(), password);

        context.restoreAuthSystemState();

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", id))
            .andExpect(status().isOk());

        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", "0000-1111-2222-3333")));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.authenticated", "authenticated")));
        assertThat(getOrcidAccessToken(profileItem), notNullValue());

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasJsonPath("$.metadata", matchMetadataNotEmpty("dspace.object.owner"))));

        getClient(authToken).perform(delete("/api/eperson/profiles/{id}", id))
            .andExpect(status().isNoContent());

        profileItem = context.reloadEntity(profileItem);

        assertThat(profileItem.getMetadata(), not(hasItem(with("person.identifier.orcid", "0000-1111-2222-3333"))));
        assertThat(profileItem.getMetadata(), not(hasItem(with("dspace.orcid.authenticated", "authenticated"))));
        assertThat(getOrcidAccessToken(profileItem), nullValue());

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
    public void testNoAutomaticProfileClaimOccursIfItemHasNotAnEmail() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withNameInMetadata("Test", "User")
            .withPassword(password)
            .withEmail("test@email.it")
            .build();

        ItemBuilder.createItem(context, personCollection)
            .withPersonIdentifierFirstName("Test")
            .withPersonIdentifierLastName("User")
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
    public void researcherProfileClaimWithoutEmail() throws Exception {

        context.turnOffAuthorisationSystem();

        final Item person = ItemBuilder.createItem(context, personCollection)
            .withTitle("Test User 1")
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(user.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
            .contentType(TEXT_URI_LIST)
            .content("http://localhost:8080/server/api/core/items/" + person.getID().toString()))
            .andExpect(status().isBadRequest());
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
    public void testOrcidMetadataOfEpersonAreCopiedOnProfile() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated())
                            .andExpect(jsonPath("$.id", is(ePersonId.toString())))
                            .andExpect(jsonPath("$.visible", is(false)))
                            .andExpect(jsonPath("$.type", is("profile")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcid", is("0000-1111-2222-3333")))
                            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")))
                            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is("DISABLED")))
                            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is("DISABLED")))
                            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences", empty()));

        String itemId = getItemIdByProfileId(authToken, ePersonId);

        Item profileItem = itemService.find(context, UUIDUtils.fromString(itemId));
        assertThat(profileItem, notNullValue());

        List<MetadataValue> metadata = profileItem.getMetadata();
        assertThat(metadata, hasItem(with("person.identifier.orcid", "0000-1111-2222-3333")));
        assertThat(metadata, hasItem(with("dspace.orcid.scope", "/first-scope", 0)));
        assertThat(metadata, hasItem(with("dspace.orcid.scope", "/second-scope", 1)));

        assertThat(getOrcidAccessToken(profileItem), is("af097328-ac1c-4a3e-9eb4-069897874910"));

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
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/publications", ALL.name()));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/publications", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceForFundings() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/fundings", ALL.name()));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is(ALL.name())));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.fundingsPreference", is(ALL.name())));

        operations = asList(new ReplaceOperation("/orcid/fundings", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
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
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/profile", "IDENTIFIERS"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                                                containsInAnyOrder("IDENTIFIERS")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.profilePreferences",
                                                containsInAnyOrder("IDENTIFIERS")));

        operations = asList(new ReplaceOperation("/orcid/profiles", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
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
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.mode", is("BATCH")));

        operations = asList(new ReplaceOperation("/orcid/mode", "MANUAL"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}", ePersonId))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.orcidSynchronization.mode", is("MANUAL")));

        operations = asList(new ReplaceOperation("/orcid/mode", "INVALID_VALUE"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
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
                                        .withOrcidScope("/first-scope")
                                        .withOrcidScope("/second-scope")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();
        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/wrong-path", "BATCH"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
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

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/mode", "BATCH"));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId)
                                         .content(getPatchContent(operations))
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isBadRequest());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithNotOwnerUser() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(post("/api/eperson/profiles/")
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/publications", ALL.name()));

        getClient(getAuthToken(user.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePersonId)
            .content(getPatchContent(operations))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

    }

    @Test
    public void testPatchToSetOrcidSynchronizationPreferenceWithAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .withOrcidScope("/first-scope")
            .withOrcidScope("/second-scope")
            .build();

        OrcidTokenBuilder.create(context, ePerson, "af097328-ac1c-4a3e-9eb4-069897874910").build();

        context.restoreAuthSystemState();

        String ePersonId = ePerson.getID().toString();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(post("/api/eperson/profiles/")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated());

        List<Operation> operations = asList(new ReplaceOperation("/orcid/publications", ALL.name()));

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePersonId)
                .content(getPatchContent(operations))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orcidSynchronization.publicationsPreference", is(ALL.name())));

    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "disabled");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", null);

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithDisabledConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        EPerson anotherUser = EPersonBuilder.createEPerson(context)
                                            .withCanLogin(true)
                                            .withEmail("user@email.it")
                                            .withPassword(password)
                                            .withNameInMetadata("Another", "User")
                                            .build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), empty());
        assertThat(getOrcidAccessToken(profile), nullValue());
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithOnlyOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), empty());
        assertThat(getOrcidAccessToken(profile), nullValue());
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithOnlyAdminConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "only_admin");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
    }

    @Test
    public void testOwnerPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(ePerson.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), empty());
        assertThat(getOrcidAccessToken(profile), nullValue());
    }

    @Test
    public void testAdminPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(ePerson.getID().toString())))
            .andExpect(jsonPath("$.visible", is(false)))
            .andExpect(jsonPath("$.type", is("profile")))
            .andExpect(jsonPath("$.orcid").doesNotExist())
            .andExpect(jsonPath("$.orcidSynchronization").doesNotExist());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), empty());
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), empty());
        assertThat(getOrcidAccessToken(profile), nullValue());
    }

    @Test
    public void testAnotherUserPatchToDisconnectProfileFromOrcidWithAdminAndOwnerConfiguration() throws Exception {

        configurationService.setProperty("orcid.disconnection.allowed-users", "admin_and_owner");

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withCanLogin(true)
                                        .withOrcid("0000-1111-2222-3333")
                                        .withOrcidScope("/read")
                                        .withOrcidScope("/write")
                                        .withEmail("test@email.it")
                                        .withPassword(password)
                                        .withNameInMetadata("Test", "User")
                                        .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        Item profile = createProfile(ePerson);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));

        context.restoreAuthSystemState();

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", ePerson.getID().toString())
                         .content(getPatchContent(asList(new RemoveOperation("/orcid"))))
                         .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        profile = context.reloadEntity(profile);

        assertThat(getMetadataValues(profile, "person.identifier.orcid"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.scope"), not(empty()));
        assertThat(getMetadataValues(profile, "dspace.orcid.authenticated"), not(empty()));
        assertThat(getOrcidAccessToken(profile), is("3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4"));
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

    @Test
    public void testOrcidSynchronizationPreferenceUpdateForceOrcidQueueRecalculation() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType publicationType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType projectType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType personType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType orgUnitType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();

        RelationshipType isAuthorOfPublication = createRelationshipTypeBuilder(context, personType, publicationType,
            "isAuthorOfPublication", "isPublicationOfAuthor", 0, null, 0, null).build();

        RelationshipType isOrgUnitOfPerson = createRelationshipTypeBuilder(context, personType, orgUnitType,
            "isOrgUnitOfPerson", "isPersonOfOrgUnit", 0, null, 0, null).build();

        RelationshipType isProjectOfPerson = createRelationshipTypeBuilder(context, projectType, personType,
            "isProjectOfPerson", "isPersonOfProject", 0, null, 0, null).build();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
            .withCanLogin(true)
            .withOrcid("0000-1111-2222-3333")
            .withOrcidScope("/read")
            .withOrcidScope("/write")
            .withEmail("test@email.it")
            .withPassword(password)
            .withNameInMetadata("Test", "User")
            .build();

        OrcidTokenBuilder.create(context, ePerson, "3de2e370-8aa9-4bbe-8d7e-f5b1577bdad4").build();

        UUID ePersonId = ePerson.getID();

        Item profile = createProfile(ePerson);

        UUID profileItemId = profile.getID();

        Collection publications = createCollection("Publications", "Publication");

        Collection orgUnits = createCollection("OrgUnits", "OrgUnit");

        Item publication = createPublication(publications, "Test publication", profile, isAuthorOfPublication);

        Collection projects = createCollection("Projects", "Project");

        Item firstProject = createProject(projects, "First project", profile, isProjectOfPerson);
        Item secondProject = createProject(projects, "Second project", profile, isProjectOfPerson);

        createOrgUnit(orgUnits, "OrgUnit", profile, isOrgUnitOfPerson);

        context.restoreAuthSystemState();

        // no preferences configured, so no orcid queue records created
        assertThat(orcidQueueService.findByProfileItemId(context, profileItemId), empty());

        String authToken = getAuthToken(ePerson.getEmail(), password);

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/publications", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        List<OrcidQueue> queueRecords = orcidQueueService.findByProfileItemId(context, profileItemId);
        assertThat(queueRecords, hasSize(1));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(publication)));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "ALL"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        queueRecords = orcidQueueService.findByProfileItemId(context, profileItemId);
        assertThat(queueRecords, hasSize(3));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(publication)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(firstProject)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(secondProject)));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/publications", "DISABLED"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        queueRecords = orcidQueueService.findByProfileItemId(context, profileItemId);
        assertThat(queueRecords, hasSize(2));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(firstProject)));
        assertThat(queueRecords, has(orcidQueueRecordWithEntity(secondProject)));

        getClient(authToken).perform(patch("/api/eperson/profiles/{id}", ePersonId.toString())
            .content(getPatchContent(asList(new ReplaceOperation("/orcid/fundings", "DISABLED"))))
            .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        assertThat(orcidQueueService.findByProfileItemId(context, profileItemId), empty());

    }

    @Test
    public void testLinkProfileWithValidCode() throws Exception {

        String code = "123456";
        String orcid = "0000-0000-1111-2222";
        String accessToken = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
        String[] scopes = { "FirstScope", "SecondScope" };

        context.turnOffAuthorisationSystem();

        Item profileItem = createProfile(user);

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(code)).thenReturn(buildOrcidTokenResponse(orcid, accessToken, scopes));

        getClient(getAuthToken(user.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        verify(orcidClientMock).getAccessToken(code);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", orcid)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", scopes[0], 0)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", scopes[1], 1)));

        assertThat(getOrcidAccessToken(profileItem), is(accessToken));

        user = context.reloadEntity(user);
        assertThat(user.getNetid(), is(orcid));
    }

    @Test
    public void testLinkProfileWithAdmin() throws Exception {

        String code = "123456";
        String orcid = "0000-0000-1111-2222";
        String accessToken = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
        String[] scopes = { "FirstScope", "SecondScope" };

        context.turnOffAuthorisationSystem();

        Item profileItem = createProfile(user);

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(code)).thenReturn(buildOrcidTokenResponse(orcid, accessToken, scopes));

        getClient(getAuthToken(admin.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isOk());

        verify(orcidClientMock).getAccessToken(code);
        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), hasItem(with("person.identifier.orcid", orcid)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", scopes[0], 0)));
        assertThat(profileItem.getMetadata(), hasItem(with("dspace.orcid.scope", scopes[1], 1)));

        assertThat(getOrcidAccessToken(profileItem), is(accessToken));

        user = context.reloadEntity(user);
        assertThat(user.getNetid(), is(orcid));

    }

    @Test
    public void testLinkProfileWithInvalidCode() throws Exception {

        String code = "123456";

        context.turnOffAuthorisationSystem();

        Item profileItem = createProfile(user);

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(code)).thenThrow(new OrcidClientException(400, "{\n" +
            "    \"error\": \"invalid_grant\",\n" +
            "    \"error_description\": \"Invalid authorization code: 123456\"\n" +
            "}"));

        getClient(getAuthToken(user.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnprocessableEntity());

        verify(orcidClientMock).getAccessToken(code);
        verifyNoMoreInteractions(orcidClientMock);

        assertThat(getOrcidAccessToken(profileItem), nullValue());
    }

    @Test
    public void testLinkProfileWithGenericError() throws Exception {

        String code = "123456";

        context.turnOffAuthorisationSystem();

        Item profileItem = createProfile(user);

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(code)).thenThrow(new OrcidClientException(401, "Forbidden"));

        getClient(getAuthToken(user.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isInternalServerError());

        verify(orcidClientMock).getAccessToken(code);
        verifyNoMoreInteractions(orcidClientMock);

        assertThat(getOrcidAccessToken(profileItem), nullValue());
    }

    @Test
    public void testLinkProfileForbiddenForNotOwnerUser() throws Exception {

        String code = "123456";
        String orcid = "0000-0000-1111-2222";
        String accessToken = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
        String[] scopes = { "FirstScope", "SecondScope" };

        context.turnOffAuthorisationSystem();

        Item profileItem = createProfile(user);

        context.restoreAuthSystemState();

        when(orcidClientMock.getAccessToken(code)).thenReturn(buildOrcidTokenResponse(orcid, accessToken, scopes));

        getClient(getAuthToken(anotherUser.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden());

        verifyNoMoreInteractions(orcidClientMock);

        profileItem = context.reloadEntity(profileItem);
        assertThat(profileItem, notNullValue());
        assertThat(profileItem.getMetadata(), not(hasItem(with("person.identifier.orcid", orcid))));
        assertThat(profileItem.getMetadata(), not(hasItem(with("dspace.orcid.scope", scopes[0], 0))));
        assertThat(profileItem.getMetadata(), not(hasItem(with("dspace.orcid.scope", scopes[1], 1))));

        assertThat(getOrcidAccessToken(profileItem), nullValue());
    }

    @Test
    public void testLinkProfileWithEPersonWithoutProfile() throws Exception {

        String code = "123456";
        String orcid = "0000-0000-1111-2222";
        String accessToken = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";
        String[] scopes = { "FirstScope", "SecondScope" };

        when(orcidClientMock.getAccessToken(code)).thenReturn(buildOrcidTokenResponse(orcid, accessToken, scopes));

        getClient(getAuthToken(user.getEmail(), password))
            .perform(patch("/api/eperson/profiles/{id}", user.getID().toString())
                .content(getPatchContent(asList(new AddOperation("/orcid", code))))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isNotFound());

        verifyNoMoreInteractions(orcidClientMock);
    }

    private Item createProfile(EPerson ePerson) throws Exception {

        String authToken = getAuthToken(ePerson.getEmail(), password);

        AtomicReference<UUID> ePersonIdRef = new AtomicReference<UUID>();
        AtomicReference<UUID> itemIdRef = new AtomicReference<UUID>();

        getClient(authToken).perform(post("/api/eperson/profiles/")
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isCreated())
                            .andDo(result -> ePersonIdRef.set(fromString(read(result.getResponse().getContentAsString(),
                                                                              "$.id"))));

        getClient(authToken).perform(get("/api/eperson/profiles/{id}/item", ePersonIdRef.get())
                                         .contentType(MediaType.APPLICATION_JSON_VALUE))
                            .andExpect(status().isOk())
                            .andDo(result -> itemIdRef.set(fromString(read(result.getResponse().getContentAsString(),
                                                                           "$.id"))));

        return itemService.find(context, itemIdRef.get());
    }

    private String getItemIdByProfileId(String token, String id) throws SQLException, Exception {
        MvcResult result = getClient(token).perform(get("/api/eperson/profiles/{id}/item", id))
                                           .andExpect(status().isOk())
                                           .andReturn();

        return readAttributeFromResponse(result, "$.id");
    }

    private String getOrcidAccessToken(Item item) {
        OrcidToken orcidToken = orcidTokenService.findByProfileItem(context, item);
        return orcidToken != null ? orcidToken.getAccessToken() : null;
    }

    private List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private Collection createCollection(String name, String entityType) throws SQLException {
        return CollectionBuilder.createCollection(context, context.reloadEntity(parentCommunity))
            .withName(name)
            .withEntityType(entityType)
            .build();
    }

    private Item createPublication(Collection collection, String title, Item author,
        RelationshipType isAuthorOfPublication) {

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withAuthor(author.getName())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, author, publication, isAuthorOfPublication).build();

        return publication;

    }

    private Item createOrgUnit(Collection collection, String title, Item person,
        RelationshipType isOrgUnitOfPerson) {

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, person, orgUnit, isOrgUnitOfPerson).build();

        return orgUnit;

    }

    private Item createProject(Collection collection, String title, Item person,
        RelationshipType isProjectOfPerson) {

        Item project = ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, project, person, isProjectOfPerson).build();

        return project;

    }

    private Predicate<OrcidQueue> orcidQueueRecordWithEntity(Item entity) {
        return orcidQueue -> entity.equals(orcidQueue.getEntity());
    }

    private <T> T readAttributeFromResponse(MvcResult result, String attribute) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), attribute);
    }

    private OrcidTokenResponseDTO buildOrcidTokenResponse(String orcid, String accessToken, String[] scopes) {
        OrcidTokenResponseDTO token = new OrcidTokenResponseDTO();
        token.setAccessToken(accessToken);
        token.setOrcid(orcid);
        token.setTokenType("Bearer");
        token.setName("Test User");
        token.setScope(String.join(" ", scopes));
        return token;
    }

}
