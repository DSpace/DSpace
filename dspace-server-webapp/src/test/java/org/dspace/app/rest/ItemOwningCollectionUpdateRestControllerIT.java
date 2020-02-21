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

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.junit.Test;

public class ItemOwningCollectionUpdateRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void moveItemTestByAnonymous() throws Exception {

        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();


        //When we call this owningCollection/move endpoint
        getClient().perform(
                put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                        ))
                //We expect a 401 Unauthorized status when performed by anonymous
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void moveItemTestByAuthorizedUser() throws Exception {

        //Turn off the authorization system, otherwise we can't make the objects
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        //2. A public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        String token = getAuthToken(admin.getEmail(), password);


        //When we call this owningCollection/move endpoint
        getClient(token)
                .perform(put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                        ))

                //We expect a 401 Unauthorized status when performed by anonymous
                .andExpect(status().isOk());
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/owningCollection")
                   .param("projection", "full"))
                   .andExpect(jsonPath("$",
                                       is(CollectionMatcher
                                                  .matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())
                                       )));
    }

    /**
     * The user minimally needs ADD for the new collection, ADMIN for the old collection and WRITE for the item to move.
     * This test will verify that these rights are sufficient
     *
     * @throws Exception
     */
    @Test
    public void moveItemTestByMinimallyAuthorizedUser() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        EPerson itemMoveEperson = EPersonBuilder.createEPerson(context).withEmail("item@move.org").withPassword("test")
                                                .withNameInMetadata("Item", "Move").build();

        ResourcePolicy rp1 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADMIN)
                                                  .withDspaceObject(col1).build();
        ResourcePolicy rp2 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.WRITE)
                                                  .withDspaceObject(publicItem1).build();
        ResourcePolicy rp3 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADD)
                                                  .withDspaceObject(col2).build();

        String token = getAuthToken(itemMoveEperson.getEmail(), "test");

        getClient(token)
                .perform(put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                        .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                        .content(
                                "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                        ))

                //We expect a 401 Unauthorized status when performed by anonymous
                .andExpect(status().isOk());
        getClient().perform(get("/api/core/items/" + publicItem1.getID() + "/owningCollection")
                   .param("projection", "full"))
                   .andExpect(jsonPath("$",
                                       is(CollectionMatcher
                                                  .matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())
                                       )));


    }

    @Test
    public void moveItemTestByAuthorizedUserWithoutAdd() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        EPerson itemMoveEperson = EPersonBuilder.createEPerson(context).withEmail("item@move.org").withPassword("test")
                                                .withNameInMetadata("Item", "Move").build();

        ResourcePolicy rp1 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADMIN)
                                                  .withDspaceObject(col1).build();
        ResourcePolicy rp2 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.WRITE)
                                                  .withDspaceObject(publicItem1).build();


        String token = getAuthToken(itemMoveEperson.getEmail(), "test");

        getClient(token).perform(put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(
                        "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                ))

                        //We expect a 401 Unauthorized status when performed by anonymous
                        .andExpect(status().isForbidden());


    }

    @Test
    public void moveItemTestByAuthorizedUserWithoutAdmin() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        EPerson itemMoveEperson = EPersonBuilder.createEPerson(context).withEmail("item@move.org").withPassword("test")
                                                .withNameInMetadata("Item", "Move").build();

        ResourcePolicy rp2 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.WRITE)
                                                  .withDspaceObject(publicItem1).build();
        ResourcePolicy rp3 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADD)
                                                  .withDspaceObject(col2).build();


        String token = getAuthToken(itemMoveEperson.getEmail(), "test");

        getClient(token).perform(put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(
                        "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                ))

                        //We expect a 401 Unauthorized status when performed by anonymous
                        .andExpect(status().isForbidden());


    }

    @Test
    public void moveItemTestByAuthorizedUserWithoutWrite() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald")
                                      .build();

        EPerson itemMoveEperson = EPersonBuilder.createEPerson(context).withEmail("item@move.org").withPassword("test")
                                                .withNameInMetadata("Item", "Move").build();

        ResourcePolicy rp1 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADMIN)
                                                  .withDspaceObject(col1).build();
        ResourcePolicy rp3 = ResourcePolicyBuilder.createResourcePolicy(context).withUser(itemMoveEperson)
                                                  .withAction(Constants.ADD)
                                                  .withDspaceObject(col2).build();


        String token = getAuthToken(itemMoveEperson.getEmail(), "test");

        getClient(token).perform(put("/api/core/items/" + publicItem1.getID() + "/owningCollection/")
                .contentType(parseMediaType(TEXT_URI_LIST_VALUE))
                .content(
                        "https://localhost:8080/spring-rest/api/core/collections/" + col2.getID()
                ))

                        //We expect a 401 Unauthorized status when performed by anonymous
                        .andExpect(status().isForbidden());


    }
}
