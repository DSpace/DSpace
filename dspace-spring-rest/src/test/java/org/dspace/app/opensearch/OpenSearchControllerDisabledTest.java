/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.opensearch;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.mock.web.MockHttpServletResponse;

/*
import org.dspace.app.rest.OpenSearchController;
import org.dspace.services.ConfigurationService;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BrowseEntryResourceMatcher;
import org.dspace.app.rest.matcher.BrowseIndexMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.util.service.OpenSearchService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
*/
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;
import org.junit.Before;
import org.springframework.test.web.servlet.MockMvc;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test to test the /opensearch endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Oliver Goldschmidt (o dot goldschmidt at tuhh dot de)
 */
public class OpenSearchControllerDisabledTest extends AbstractControllerIntegrationTest {

    private ConfigurationService configurationService;

    @Before
    public void init() throws Exception {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("websvc.opensearch.enable", false);
/*
System.out.println("Testing OpenSearch");
MockHttpServletResponse resp2 = getClient().perform(get("/opensearch/search")
                                .param("query", "dog"))
                               .andReturn().getResponse();
System.out.println("Response from Test 2: "+resp2.getContentAsString());
*/
    }

    @Test
    public void searchTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "dog"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType("text/html"))
/*
                   //Our default Discovery config has 4 browse indexes so we expect this to be reflected in the page
                   // object
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
*/
        ;
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/service"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   .andExpect(content().contentType("text/html"))
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
//                   .andExpect(content().contentType(contentType))
/*
                   //Check that the JSON root matches the expected browse index
                   .andExpect(jsonPath("$", BrowseIndexMatcher.titleBrowseIndex("asc")))
*/
        ;
    }

/*
    @Test
    public void findItemsWithOU() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
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
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find which subjects are currently in the repository
        getClient().perform(get("/api/discover/browses/subject/entries"))

                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())

                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Check that there are indeed 3 different subjects
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   //Check the embedded resources and that they're sorted alphabetically
                   //Check that the subject matches as expected
                   //Verify that they're sorted alphabetically
                   .andExpect(jsonPath("$._embedded.browseEntries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("ExtraEntry", 3),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("TestingForMore", 2)
                                       )));

        getClient().perform(get("/api/discover/browses/subject/entries")
                                .param("sort", "value,desc"))

                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())
                   .andDo(MockMvcResultHandlers.print())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Check that there are indeed 3 different subjects
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   //Check the embedded resources and that they're sorted alphabetically
                   //Check that the subject matches as expected
                   //Verify that they're sorted alphabetically
                   .andExpect(jsonPath("$._embedded.browseEntries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("TestingForMore", 2),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("ExtraEntry", 3),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1)
                                       )));
    }
*/
}