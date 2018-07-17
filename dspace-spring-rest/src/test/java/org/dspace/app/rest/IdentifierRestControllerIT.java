/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.CollectionMatcher;
import org.dspace.app.rest.matcher.CommunityMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for the identifier resolver
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class IdentifierRestControllerIT extends AbstractControllerIntegrationTest {

    @Before
    public void setup() throws Exception {
        super.setUp();
    }

    @Test
    public void testValidIdentifier() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        context.restoreAuthSystemState();

        String handle = parentCommunity.getHandle();
        String communityDetail = REST_SERVER_URL + "core/communities/" + parentCommunity.getID();

        getClient().perform(get("/api/pid/find?id={handle}", handle))
                   .andExpect(status().isFound())
                   //We expect a Location header to redirect to the community details
                   .andExpect(header().string("Location", communityDetail));
    }

    @Test
    public void testUnexistentIdentifier() throws Exception {
        getClient().perform(get("/api/pid/find?{id}", "fakeIdentifier"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findItemThroughUUIDTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String itemDetails = REST_SERVER_URL + "core/items/" + publicItem1.getID();


        getClient().perform(get("/api/pid/find?id=" + publicItem1.getID()))
                   .andExpect(status().isFound())
                   //We expect a Location header to redirect to the community details
                   .andExpect(header().string("Location", itemDetails));
    }

    @Test
    public void findCollectionThroughUUIDTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String collectionDetails = REST_SERVER_URL + "core/collections/" + col1.getID();


        getClient().perform(get("/api/pid/find?id={uuid}", col1.getID()))
                   .andExpect(status().isFound())
                   //We expect a Location header to redirect to the community details
                   .andExpect(header().string("Location", collectionDetails));
    }

    @Test
    public void findCommunityThroughUUIDTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        // We create a top community to receive an identifier
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();

        String communityDetails = REST_SERVER_URL + "core/communities/" + parentCommunity.getID();

        getClient().perform(get("/api/pid/find?id={uuid}", parentCommunity.getID()))
                   .andExpect(status().isFound())
                   //We expect a Location header to redirect to the community details
                   .andExpect(header().string("Location", communityDetails));
    }
}
