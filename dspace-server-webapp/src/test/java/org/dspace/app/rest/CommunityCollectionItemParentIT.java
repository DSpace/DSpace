/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityCollectionItemParentIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    @Autowired
    private AuthorizeService authorizeService;

    Community communityA;
    Community communityB;
    Community communityAA;
    Community communityAB;

    Collection colAA1;
    Collection colAA2;
    Collection colAB1;

    Item itemAA1;
    Item itemAA1MappedInAA2;
    Item itemAA2;


    @Before
    public void setup() throws SQLException, AuthorizeException {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();
        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        communityA = CommunityBuilder.createCommunity(context)
                                     .withName("Parent CommunityA")
                                     .build();
        communityB = CommunityBuilder.createCommunity(context)
                                     .withName("Parent CommunityB")
                                     .build();
        communityAA = CommunityBuilder.createSubCommunity(context, communityA)
                                      .withName("Sub Community")
                                      .build();
        communityAB = CommunityBuilder.createSubCommunity(context, communityA)
                                      .withName("Sub Community Two")
                                      .build();
        colAA1 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 1").build();
        colAA2 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 2").build();
        colAB1 = CollectionBuilder.createCollection(context, communityAB).withName("Collection 3").build();
        communityService.addCollection(context, communityAB, colAA2);


        itemAA1 = ItemBuilder.createItem(context, colAA1)
                             .withTitle("Public item 1")
                             .withIssueDate("2017-10-17")
                             .withAuthor("Smith, Donald").withAuthor("Doe, John")
                             .withSubject("ExtraEntry")
                             .build();

        itemAA1MappedInAA2 = ItemBuilder.createItem(context, colAA1)
                                        .withTitle("Public item 1")
                                        .withIssueDate("2017-10-17")
                                        .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                        .withSubject("ExtraEntry")
                                        .build();
        collectionService.addItem(context, colAA2, itemAA1MappedInAA2);
        itemAA2 = ItemBuilder.createItem(context, colAA2)
                             .withTitle("Public item 1")
                             .withIssueDate("2017-10-17")
                             .withAuthor("Smith, Donald").withAuthor("Doe, John")
                             .withSubject("ExtraEntry")
                             .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void itemAA1OwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemAA1.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(colAA1.getName(),
                                                                                           colAA1.getID(),
                                                                                           colAA1.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(colAA2.getName(), colAA2.getID(), colAA2.getHandle())))));

    }

    @Test
    public void itemAA1MappedInAA2OwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemAA1MappedInAA2.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(colAA1.getName(),
                                                                                           colAA1.getID(),
                                                                                           colAA1.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(colAA2.getName(), colAA2.getID(), colAA2.getHandle())))));
    }

    @Test
    public void itemAA2OwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemAA2.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(colAA2.getName(),
                                                                                           colAA2.getID(),
                                                                                           colAA2.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(colAA1.getName(), colAA1.getID(), colAA1.getHandle())))));

    }

    @Test
    public void colAA1ParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + colAA1.getID() + "/parentCommunity"))
                        .andExpect(jsonPath("$", is(CommunityMatcher
                                                        .matchCommunityEntry(communityAA.getName(), communityAA.getID(),
                                                                             communityAA.getHandle()))))
                        .andExpect(jsonPath("$", not(is(CommunityMatcher.matchCommunityEntry(communityA.getName(),
                                                                                             communityA.getID(),
                                                                                             communityA.getHandle())))))
                        .andExpect(jsonPath("$", not(is(CommunityMatcher.matchCommunityEntry(communityAB.getName(),
                                                                                             communityAB.getID(),
                                                                                             communityAB
                                                                                                 .getHandle())))));

    }

    @Test
    public void comAAParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
            .perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity"))
            .andExpect(jsonPath("$", Matchers
                .is(CommunityMatcher.matchCommunityEntry(communityA.getID(), communityA.getHandle()))))
            .andExpect(jsonPath("$", Matchers
                .not(Matchers.is(CommunityMatcher.matchCommunityEntry(communityB.getID(), communityB.getHandle())))));

    }

    @Test
    public void comAParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + communityA.getID() + "/parentCommunity"))
                        .andExpect(status().isNoContent());


    }

    @Test
    public void parentCommunityWrongUUIDTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + UUID.randomUUID() + "/parentCommunity"))
                        .andExpect(status().isNotFound());


    }

    @Test
    public void parentCommunityPrivateCommunityUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityAA);
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity"))
            .andExpect(status().isUnauthorized());

    }

    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void parentCommunityPrivateParentCommunityUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityA);
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity"))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void parentCommunityPrivateCommunityForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityAA);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity"))
                   .andExpect(status().isForbidden());

    }

    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void parentCommunityPrivateParentCommunityForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityA);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/communities/" + communityAA.getID() + "/parentCommunity"))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void parentCommunityForCollectionWrongUUIDTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + UUID.randomUUID() + "/parentCommunity"))
                        .andExpect(status().isNotFound());


    }

    @Test
    public void parentCommunityForCollectionPrivateCollectionUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, colAA1);
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/collections/" + colAA1.getID() + "/parentCommunity"))
                   .andExpect(status().isUnauthorized());

    }

    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void parentCommunityForCollectionPrivateParentCommunityUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityAA);
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/collections/" + colAA1.getID() + "/parentCommunity"))
                   .andExpect(status().isUnauthorized());

    }

    @Test
    public void parentCommunityForCollectionPrivateCollectionForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, colAA1);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + colAA1.getID() + "/parentCommunity"))
                   .andExpect(status().isForbidden());

    }

    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void parentCommunityForCollectionPrivateParentCommunityForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, communityAA);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + colAA1.getID() + "/parentCommunity"))
                        .andExpect(status().isForbidden());

    }

    @Test
    public void owningCollectionForItemWrongUUIDTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + UUID.randomUUID() + "/owningCollection"))
                        .andExpect(status().isNotFound());


    }

    @Test
    public void owningCollectionForItemPrivateItemUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemAA1);
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/items/" + itemAA1.getID() + "/owningCollection"))
                   .andExpect(status().isUnauthorized());

    }


    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void owningCollectionForItemPrivateOwningCollectionUnAuthorizedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, colAA1);
        context.restoreAuthSystemState();
        getClient().perform(get("/api/core/items/" + itemAA1.getID() + "/owningCollection"))
                   .andExpect(status().isUnauthorized());

    }

    //Enable this test when this security level has been supported
    @Ignore
    @Test
    public void owningCollectionForItemPrivateOwningCollectionForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, colAA1);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemAA1.getID() + "/owningCollection"))
                   .andExpect(status().isForbidden());

    }
    @Test
    public void owningCollectionForItemPrivateItemForbiddenTest() throws Exception {

        context.turnOffAuthorisationSystem();
        authorizeService.removeAllPolicies(context, itemAA1);
        context.restoreAuthSystemState();
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemAA1.getID() + "/owningCollection"))
                   .andExpect(status().isForbidden());

    }

}
