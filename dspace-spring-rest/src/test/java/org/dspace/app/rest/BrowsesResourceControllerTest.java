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
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BrowseIndexMatchers;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

/**
 * Integration test to test the /api/discover/browses endpoint
 */
public class BrowsesResourceControllerTest extends AbstractControllerIntegrationTest {

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
                        BrowseIndexMatchers.dateIssuedBrowseIndex("asc"),
                        BrowseIndexMatchers.contributorBrowseIndex("asc"),
                        BrowseIndexMatchers.titleBrowseIndex("asc"),
                        BrowseIndexMatchers.subjectBrowseIndex("asc")
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
                .andExpect(jsonPath("$", BrowseIndexMatchers.titleBrowseIndex("asc")))
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
                .andExpect(jsonPath("$", BrowseIndexMatchers.dateIssuedBrowseIndex("asc")))
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
                .andExpect(jsonPath("$", BrowseIndexMatchers.contributorBrowseIndex("asc")))
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
                .andExpect(jsonPath("$", BrowseIndexMatchers.subjectBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByTitleItems() throws Exception {

        context.turnOffAuthorisationSystem();
        parentCommunity = new CommunityBuilder().createCommunity(context)
                .withName("Parent Community")
                .build();

        Community child1 = new CommunityBuilder().createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();

        Collection col1 = new CollectionBuilder().createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = new CollectionBuilder().createCollection(context, child1).withName("Collection 2").build();

        Item item1 = new ItemBuilder().createItem(context, col1)
                .withTitle("My first test item")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withAuthor("Doe, John")
                .withSubject("Java")
                .withSubject("Unit Testing")
                .build();

        Item item2 = new ItemBuilder().createItem(context, col2)
                .withTitle("My second test item")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria")
                .withAuthor("Doe, Jane")
                .withSubject("Angular")
                .withSubject("Unit Testing")
                .build();

        context.restoreAuthSystemState();

        //When we browse the items in the Browse by item endpoint
        mockMvc.perform(get("/api/discover/browses/title/items"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))

                //We expect our two created items to be present
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
        ;
    }
}