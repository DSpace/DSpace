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

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class CommunityCollectionParentIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CommunityService communityService;

    Community communityA;
    Community communityB;
    Community communityAA;
    Community communityAB;

    Collection col1;
    Collection col2;
    Collection col3;

    Item itemX;
    Item itemY;
    Item itemZ;


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
        col1 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 1").build();
        col2 = CollectionBuilder.createCollection(context, communityAA).withName("Collection 2").build();
        col3 = CollectionBuilder.createCollection(context, communityAB).withName("Collection 3").build();
        communityService.addCollection(context, communityAB, col2);


        itemX = ItemBuilder.createItem(context, col1)
                           .withTitle("Public item 1")
                           .withIssueDate("2017-10-17")
                           .withAuthor("Smith, Donald").withAuthor("Doe, John")
                           .withSubject("ExtraEntry")
                           .build();

        itemY = ItemBuilder.createItem(context, col1)
                           .withTitle("Public item 1")
                           .withIssueDate("2017-10-17")
                           .withAuthor("Smith, Donald").withAuthor("Doe, John")
                           .withSubject("ExtraEntry")
                           .build();
        collectionService.addItem(context, col2, itemY);
        itemZ = ItemBuilder.createItem(context, col2)
                           .withTitle("Public item 1")
                           .withIssueDate("2017-10-17")
                           .withAuthor("Smith, Donald").withAuthor("Doe, John")
                           .withSubject("ExtraEntry")
                           .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void itemXOwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemX.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(),
                                                                                           col1.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())))));

    }

    @Test
    public void itemYOwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemY.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(col1.getName(), col1.getID(),
                                                                                           col1.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(col2.getName(), col2.getID(), col2.getHandle())))));
    }

    @Test
    public void itemZOwningCollectionTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/items/" + itemZ.getID() + "/owningCollection"))
                        .andExpect(jsonPath("$", is(CollectionMatcher.matchCollectionEntry(col2.getName(), col2.getID(),
                                                                                           col2.getHandle()))))
                        .andExpect(jsonPath("$", Matchers
                            .not(is(CollectionMatcher
                                        .matchCollectionEntry(col1.getName(), col1.getID(), col1.getHandle())))));

    }

    @Test
    public void col1ParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/collections/" + col1.getID() + "/parentCommunity"))
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
    public void col2ParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
            .perform(get("/api/core/collections/" + col2.getID() + "/parentCommunity"))
            .andExpect(jsonPath("$", is(CommunityMatcher.matchCommunityEntry(communityAA.getName(), communityAA.getID(),
                                                                             communityAA.getHandle()))))
            .andExpect(jsonPath("$", not(is(CommunityMatcher
                                                .matchCommunityEntry(communityA.getName(), communityA.getID(),
                                                                     communityA.getHandle())))))
            .andExpect(jsonPath("$", not(is(CommunityMatcher
                                                .matchCommunityEntry(communityAB.getName(), communityAB.getID(),
                                                                     communityAB.getHandle())))));

    }

    @Test
    public void col3ParentCommunityTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token)
            .perform(get("/api/core/collections/" + col3.getID() + "/parentCommunity"))
            .andExpect(jsonPath("$", is(CommunityMatcher.matchCommunityEntry(communityAB.getName(), communityAB.getID(),
                                                                             communityAB.getHandle()))))
            .andExpect(jsonPath("$", not(is(CommunityMatcher
                                                .matchCommunityEntry(communityA.getName(), communityA.getID(),
                                                                     communityA.getHandle())))))
            .andExpect(jsonPath("$", not(is(CommunityMatcher
                                                .matchCommunityEntry(communityAA.getName(), communityAA.getID(),
                                                                     communityAA.getHandle())))));

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

}
