/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ResourcePolicyBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.junit.Test;

public class CollectionOwningCommunityUpdateRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void moveCollectionToSameCommunityBadRequest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col = CollectionBuilder.createCollection(context, parent).withName("Collection 1").build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        // Try moving collection into its current community
        getClient(token).perform(
                put("/api/core/collections/" + col.getID() + "/owningCommunity")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/communities/" + parent.getID())
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void moveNonExistentCollectionNotFound() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent = CommunityBuilder.createCommunity(context).withName("Target Community").build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        // Random UUID that doesn’t exist in DB
        String fakeUuid = "00000000-0000-0000-0000-000000000000";

        getClient(token).perform(
                put("/api/core/collections/" + fakeUuid + "/owningCommunity")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/communities/" + parent.getID())
        ).andExpect(status().isNotFound());
    }

    @Test
    public void moveCollectionTestByAnonymous() throws Exception {

        context.turnOffAuthorisationSystem();

        Community parent1 = CommunityBuilder.createCommunity(context).withName("Parent Community 1").build();
        Community parent2 = CommunityBuilder.createCommunity(context).withName("Parent Community 2").build();

        Collection col1 = CollectionBuilder.createCollection(context, parent1).withName("Collection 1").build();

        context.restoreAuthSystemState();

        // Anonymous Attempt - should Fail
        getClient().perform(
            put("/api/core/collections/" + col1.getID() + "/owningCommunity")
            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
            .content("https://localhost:8080/spring-rest/api/core/communities/" + parent2.getID())
        ).andExpect(status().isUnauthorized());
    }

    @Test
    public void moveCollectionTestByAdmin() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent1 = CommunityBuilder.createCommunity(context).withName("Parent Community 1").build();
        Community parent2 = CommunityBuilder.createCommunity(context).withName("Parent Community 2").build();

        Collection col1 = CollectionBuilder.createCollection(context, parent1).withName("Collection 1").build();

        context.turnOffAuthorisationSystem();

        String token = getAuthToken(admin.getEmail(), password);

        // Move as admin - should succeed
        getClient(token).perform(
            put("/api/core/collections/" + col1.getID() + "/owningCommunity")
            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
            .content("https://localhost:8080/spring-rest/api/core/communities/" + parent2.getID())
        ).andExpect(status().isOk());

        // verify that the moved collection is under parent 2
        getClient(token).perform(get("/api/core/communities/" + parent2.getID() + "/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections[0]",
            is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle())))
        );

        // verify that the moved collection is not under parent 1
        getClient(token).perform(get("/api/core/communities/" + parent1.getID() + "/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections").isEmpty());
    }

    @Test
    public void moveCollectionTestByMinimallyAuthorizedUser() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent1 = CommunityBuilder.createCommunity(context).withName("Parent Community 1").build();
        Community parent2 = CommunityBuilder.createCommunity(context).withName("Parent Community 2").build();

        Collection col1 = CollectionBuilder.createCollection(context, parent1).withName("Collection 1").build();

        EPerson eperson = EPersonBuilder.createEPerson(context)
            .withEmail("move@collection.org").withPassword("test").build();

        // Required Permissions
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withAction(Constants.ADMIN).withDspaceObject(parent1).build();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withAction(Constants.WRITE).withDspaceObject(col1).build();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
            .withAction(Constants.ADD).withDspaceObject(parent2).build();

        context.turnOffAuthorisationSystem();

        String token = getAuthToken(eperson.getEmail(), "test");

        getClient(token).perform(
            put("/api/core/collections/" + col1.getID() + "/owningCommunity")
            .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
            .content("https://localhost:8080/spring-rest/api/core/communities/" + parent2.getID())
        ).andExpect(status().isOk());

        getClient(token).perform(get("/api/core/communities/" + parent2.getID() + "/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.collections[0]",
            is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle())))
        );
    }

    @Test
    public void moveCollectionForbiddenWithoutAdd() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent1 = CommunityBuilder.createCommunity(context).withName("Parent Community 1").build();
        Community parent2 = CommunityBuilder.createCommunity(context).withName("Parent Community 2").build();
        Collection col1 = CollectionBuilder.createCollection(context, parent1).withName("Collection 1").build();

        EPerson eperson = EPersonBuilder.createEPerson(context)
                .withEmail("move@collection.org").withPassword("test").build();

        // Missing ADD on target
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                .withAction(Constants.ADMIN).withDspaceObject(parent1).build();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                .withAction(Constants.WRITE).withDspaceObject(col1).build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), "test");

        getClient(token).perform(
                put("/api/core/collections/" + col1.getID() + "/owningCommunity")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/communities/" + parent2.getID())
        ).andExpect(status().isForbidden());
    }

    @Test
    public void moveCollectionForbiddenWithoutAdminOnSource() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parent1 = CommunityBuilder.createCommunity(context).withName("Parent Community 1").build();
        Community parent2 = CommunityBuilder.createCommunity(context).withName("Parent Community 2").build();
        Collection col1 = CollectionBuilder.createCollection(context, parent1).withName("Collection 1").build();

        EPerson eperson = EPersonBuilder.createEPerson(context)
                .withEmail("move@collection.org").withPassword("test").build();

        // Missing ADMIN on source
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                .withAction(Constants.WRITE).withDspaceObject(col1).build();
        ResourcePolicyBuilder.createResourcePolicy(context, eperson, null)
                .withAction(Constants.ADD).withDspaceObject(parent2).build();

        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), "test");

        getClient(token).perform(
                put("/api/core/collections/" + col1.getID() + "/owningCommunity")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content("https://localhost:8080/spring-rest/api/core/communities/" + parent2.getID())
        ).andExpect(status().isForbidden());
    }
}
