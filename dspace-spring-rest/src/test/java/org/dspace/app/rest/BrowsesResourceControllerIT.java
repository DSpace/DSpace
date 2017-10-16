/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BrowseIndexMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.junit.Test;

/**
 * Integration test to test the /api/discover/browses endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class BrowsesResourceControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Our default Discovery config has 4 browse indexes so we expect this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(4)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0)))

                //The array of browse index should have a size 4
                .andExpect(jsonPath("$._embedded.browses", hasSize(4)))

                //Check that all (and only) the default browse indexes are present
                .andExpect(jsonPath("$._embedded.browses", containsInAnyOrder(
                        BrowseIndexMatcher.dateIssuedBrowseIndex("asc"),
                        BrowseIndexMatcher.contributorBrowseIndex("asc"),
                        BrowseIndexMatcher.titleBrowseIndex("asc"),
                        BrowseIndexMatcher.subjectBrowseIndex("asc")
                )))
        ;
    }

    @Test
    public void findBrowseByTitle() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses/title"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Check that the JSON root matches the expected browse index
                .andExpect(jsonPath("$", BrowseIndexMatcher.titleBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByDateIssued() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses/dateissued"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Check that the JSON root matches the expected browse index
                .andExpect(jsonPath("$", BrowseIndexMatcher.dateIssuedBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByContributor() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses/author"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Check that the JSON root matches the expected browse index
                .andExpect(jsonPath("$", BrowseIndexMatcher.contributorBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseBySubject() throws Exception {
        //When we call the root endpoint
        mockMvc.perform(get("/api/discover/browses/subject"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //Check that the JSON root matches the expected browse index
                .andExpect(jsonPath("$", BrowseIndexMatcher.subjectBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByTitleItems() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        //2. Two public items that are readable by Anonymous
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("Java").withSubject("Unit Testing")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("Angular").withSubject("Unit Testing")
                .build();

        //3. An item that has been made private
        Item privateItem = new ItemBuilder().createItem(context, col1)
                .withTitle("This is a private item")
                .withIssueDate("2015-03-12")
                .withAuthor("Duck, Donald")
                .withSubject("Cartoons").withSubject("Ducks")
                .makePrivate()
                .build();

        //4. An item with an item-level embargo
        Item embargoedItem = new ItemBuilder().createItem(context, col2)
                .withTitle("An embargoed publication")
                .withIssueDate("2017-08-10")
                .withAuthor("Mouse, Mickey")
                .withSubject("Cartoons").withSubject("Mice")
                .withEmbargoPeriod("12 months")
                .build();

        //5. An item that is only readable for an internal groups
        Group internalGroup = new GroupBuilder().createGroup(context)
                .withName("Internal Group")
                .build();

        Item internalItem = new ItemBuilder().createItem(context, col2)
                .withTitle("Internal publication")
                .withIssueDate("2016-09-19")
                .withAuthor("Doe, John")
                .withSubject("Unknown")
                .withReaderGroup(internalGroup)
                .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses the items in the Browse by item endpoint
        //sorted descending by tile
        mockMvc.perform(get("/api/discover/browses/title/items")
                .param("sort", "title,desc"))

        //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //We expect only the two public items and the embargoed item to be present
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(3)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0)))

                //Verify that the title of the public and embargoed items are present and sorted descending
                .andExpect(jsonPath("$._embedded.items",
                        contains(ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                                "Public item 2", "2016-02-13"),
                                ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                        "Public item 1", "2017-10-17"),
                                ItemMatcher.matchItemWithTitleAndDateIssued(embargoedItem,
                                        "An embargoed publication", "2017-08-10"))))

                //The private item must not be present
                .andExpect(jsonPath("$._embedded.items[*].metadata[?(@.key=='dc.title')].value",
                        not(hasItem("This is a private item"))))

                //The internal item must not be present
                .andExpect(jsonPath("$._embedded.items[*].metadata[?(@.key=='dc.title')].value",
                        not(hasItem("Internal publication"))))
        ;

        //** CLEANUP **
        context.turnOffAuthorisationSystem();
        new GroupBuilder().delete(context, internalGroup);
    }
}