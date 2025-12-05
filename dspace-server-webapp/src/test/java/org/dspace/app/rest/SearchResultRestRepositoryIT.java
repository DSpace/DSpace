/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Period;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.rest.matcher.AppliedFilterMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.SearchResultMatcher;
import org.dspace.app.rest.matcher.SortOptionMatcher;
import org.dspace.app.rest.matcher.WorkflowItemMatcher;
import org.dspace.app.rest.matcher.WorkspaceItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.ClaimedTaskBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.LDNMessageBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.builder.SupervisionOrderBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class SearchResultRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void discoverSearchByFieldNotConfiguredTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Testing, Works")
            .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("sort", "dc.date.accessioned, ASC")
                .param("configuration", "workspace"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void discoverSearchObjectsTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        getClient().perform(get("/api/discover/searchresults/search/objects"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
            )))
            //These search results have to be shown in the embedded.objects section as these are the items
            // given in the structure defined above.
            //Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "community", "communities"),
                //This has to be like this because collections don't have anything else
                SearchResultMatcher.match(),
                SearchResultMatcher.match(),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsWithSpecialCharacterTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        ItemBuilder.createItem(context, collection)
            .withAuthor("DSpace & friends")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/discover/searchresults/search/objects")
                    .param("sort", "score,DESC")
                    .param("page", "0")
                    .param("size", "10"))
            .andExpect(status().isOk());
    }

    @Test
    public void discoverSearchBrowsesWithSpecialCharacterTest() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        ItemBuilder.createItem(context, collection)
            .withAuthor("DSpace & friends")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(
                get("/api/discover/browses/author/entries")
                    .param("sort", "default,ASC")
                    .param("page", "0")
                    .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries", hasItem(allOf(
                hasJsonPath("$.value", is("DSpace & friends")),
                hasJsonPath("$._links.items.href", containsString("DSpace%20%26%20friends"))
            ))));
    }

    @Test
    public void discoverSearchObjectsTestWithBasicQuery() throws Exception {
        //We turn off the authorization system in order to create the structure defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a query that says that the title has to contain 'test'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test,contains"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object has to look like this because of the query we specified, only two elements match
            // the query.
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            //Only the two item elements match the query, therefore those are the only ones that can be in the
            // embedded.objects section
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //We need to display the appliedFilters object that contains the query that we've run
            .andExpect(jsonPath("$.appliedFilters", contains(
                AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "test"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "test:"))
            .andExpect(status().isUnprocessableEntity());

    }

    @Test
    public void discoverSearchObjectsTestWithInvalidSolrQuery() throws Exception {

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "test"))
            .andExpect(status().isOk());

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "test:"))
            .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void discoverSearchObjectsTestWithScope() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a scope 'test'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("scope", "test"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page element has to look like this because it contains all the elements we've just created
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
            )))
            //The scope property has to be set to the value we entered in the parameters
            .andExpect(jsonPath("$.scope", is("test")))
            //All the elements created in the structure above have to be present in the embedded.objects section
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "community", "communities"),
                //Collections are specified like this because they don't have any special properties
                SearchResultMatcher.match(),
                SearchResultMatcher.match(),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestWithDsoType() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        // ** WHEN **
        // An anonymous user browses this endpoint to find the objects in the system

        // With dsoType 'item'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("dsoType", "Item"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page element needs to look like this and only have three totalElements because we only want
            // the items (dsoType) and we only created three items
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
            )))
            //Only the three items can be present in the embedded.objects section as that's what we specified
            // in the dsoType parameter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        // With dsoTypes 'community' and 'collection'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("dsoType", "Community")
                .param("dsoType", "Collection"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            // The page element needs to look like this and only have four totalElements because we only want
            // the communities and the collections (dsoType) and we only created two of both types
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 4)
            )))
            // Only the two communities and the two collections can be present in the embedded.objects section
            // as that's what we specified in the dsoType parameter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "collection", "collections"),
                SearchResultMatcher.match("core", "collection", "collections")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        // With dsoTypes 'collection' and 'item'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("dsoType", "Collection")
                .param("dsoType", "Item"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            // The page element needs to look like this and only have five totalElements because we only want
            // the collections and the items (dsoType) and we only created two collections and three items
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 5)
            )))
            // Only the two collections and the three items can be present in the embedded.objects section
            // as that's what we specified in the dsoType parameter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "collection", "collections"),
                SearchResultMatcher.match("core", "collection", "collections"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        // With dsoTypes 'community', 'collection' and 'item'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("dsoType", "Community")
                .param("dsoType", "Collection")
                .param("dsoType", "Item"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            // The page element needs to look like this and have seven totalElements because we want
            // the communities, the collections and the items (dsoType) and we created two communities,
            // two collections and three items
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
            )))
            // The two communities, the two collections and the three items can be present in the embedded.objects
            // section as that's what we specified in the dsoType parameter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "collection", "collections"),
                SearchResultMatcher.match("core", "collection", "collections"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsTestWithDsoTypeAndSort() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Testing")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a dsoType 'item'
        //And a sort on the dc.title ascending
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("dsoType", "Item")
                .param("sort", "dc.title,ASC"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object has to look like this and only contain three total elements because we only want
            // to get the items back
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
            )))
            //Only the three items can be present in the embedded.objects section as that's what we specified
            // in the dsoType parameter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items"),
                SearchResultMatcher.match("core", "item", "items")
            )))
            //Here we want to match on the item name in a certain specified order because we want to check the
            // sort properly
            //We check whether the items are sorted properly as we expected
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Public"),
                SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                SearchResultMatcher.matchOnItemName("item", "items", "Testing")
            )))
            //We want to get the sort that's been used as well in the response
            .andExpect(jsonPath("$.sort", is(
                SortOptionMatcher.sortByAndOrder("dc.title", "ASC")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestForPaginationAndNextLinks() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("t, t").withAuthor("t, y")
            .withAuthor("t, r").withAuthor("t, e").withAuthor("t, z").withAuthor("t, a")
            .withAuthor("t, tq").withAuthor("t, ts").withAuthor("t, td").withAuthor("t, tf")
            .withAuthor("t, tg").withAuthor("t, th").withAuthor("t, tj").withAuthor("t, tk")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry").withSubject("a").withSubject("b").withSubject("c")
            .withSubject("d").withSubject("e").withSubject("f").withSubject("g")
            .withSubject("h").withSubject("i").withSubject("j")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("size", "2")
                .param("page", "1"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            //Size of 2 because that's what we entered
            //Page number 1 because that's the param we entered
            //TotalPages 4 because size = 2 and total elements is 7 -> 4 pages
            //We made 7 elements -> 7 total elements
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 4, 7)
            )))
            //These are the  two elements that'll be shown (because page = 1, so the third and fourth element
            // in the list) and they'll be the only ones because the size is 2
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match(),
                SearchResultMatcher.match()
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }


    @Test
    public void discoverSearchObjectsTestWithContentInABitstream() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();
        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.
                createBitstream(context, publicItem1, is)
                .withName("Bitstream")
                .withMimeType("text/plain")
                .build();
        }

        //Run the filter media to make the text in the bitstream searchable through the query
        runDSpaceScript("filter-media", "-f", "-i", publicItem1.getHandle());

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a query stating 'ThisIsSomeDummyText'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "ThisIsSomeDummyText"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //This is the only item that should be returned with the query given
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Test")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForEmbargoedItemsAndPrivateItems() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        //Make this one public to make sure that it doesn't show up in the search
        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Embargoed item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .withEmbargoPeriod(Period.ofMonths(12))
            .build();

        //Turn on the authorization again
        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //
        getClient().perform(get("/api/discover/searchresults/search/objects"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 5)
            )))
            //These are the items that aren't set to private
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                SearchResultMatcher.match("core", "community", "communities"),
                SearchResultMatcher.match("core", "community", "communities"),
                //Collections are specified like this because they don't have any special properties
                SearchResultMatcher.match(),
                SearchResultMatcher.match(),
                SearchResultMatcher.matchOnItemName("item", "items", "Test")
            )))
            //This is a private item, this shouldn't show up in the result
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.not(
                    Matchers.anyOf(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Embargoed item 2")
                    )
                )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    //TODO Enable when solr fulltext indexing is policy-aware, see https://jira.duraspace.org/browse/DS-3758
    @Test
    @Ignore
    public void discoverSearchObjectsTestWithContentInAPrivateBitstream() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. one public item that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        String bitstreamContent = "ThisIsSomeDummyText";

        //Make the group that anon doesn't have access to
        Group internalGroup = GroupBuilder.createGroup(context)
            .withName("Internal Group")
            .build();

        //Add this bitstream with the internal group as the reader group
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.
                createBitstream(context, publicItem1, is)
                .withName("Bitstream")
                .withDescription("Test Private Bitstream")
                .withMimeType("text/plain")
                .withReaderGroup(internalGroup)
                .build();
        }


        //Run the filter media to be able to search on the text in the bitstream
        runDSpaceScript("filter-media", "-f", "-i", publicItem1.getHandle());

        //Turn on the authorization again to make sure that private/inaccessible items don't get show/used
        context.restoreAuthSystemState();
        context.setCurrentUser(null);
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "ThisIsSomeDummyText"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //Make sure that the item with the private bitstream doesn't show up
            .andExpect(jsonPath("$._embedded.object", Matchers.not(Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Test")
            ))))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }


    @Test
    public void discoverSearchObjectsTestForScope() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the scope given
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("scope", String.valueOf(scope)))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The scope has to be equal to the one given in the parameters
            .andExpect(jsonPath("$.scope", is(String.valueOf(scope))))
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items belonging to the scope specified
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForScopeWithPrivateItem() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
        //2. Two items that are readable by Anonymous with different subjects and one private item
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("scope", String.valueOf(scope)))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //Make sure that the scope is set to the scope given in the param
            .andExpect(jsonPath("$.scope", is(String.valueOf(scope))))
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //Make sure that the search results contains the item with the correct scope
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2")
//                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //Make sure that the search result doesn't contain the item that's set to private but does have
            // the correct scope
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.not(
                Matchers.contains(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                ))))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    /**
     * This test verifies that
     * {@link org.dspace.discovery.indexobject.InprogressSubmissionIndexFactoryImpl#storeInprogressItemFields}
     * indexes the owning collection of workspace items.
     *
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void discoverSearchObjectsTestForWorkspaceItemInCollectionScope() throws Exception {
        // in-progress submissions are only visible to the person who created them
        context.setCurrentUser(eperson);

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("Community")
            .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
            .withName("Collection 1")
            .build();

        Collection collection2 = CollectionBuilder.createCollection(context, community)
            .withName("Collection 2")
            .build();

        WorkspaceItem wsi1 = WorkspaceItemBuilder.createWorkspaceItem(context, collection1)
            .withTitle("Workspace Item 1")
            .build();

        WorkspaceItem wsi2 = WorkspaceItemBuilder.createWorkspaceItem(context, collection2)
            .withTitle("Workspace Item 2")
            .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(ePersonToken).perform(
                get("/api/discover/searchresults/search/objects")
                    // The workspace configuration returns all items (workspace, workflow, archived) of the current user
                    // see: https://github.com/DSpace/RestContract/blob/main/search-endpoint.md#workspace
                    .param("configuration", "workspace")
                    .param("scope", collection1.getID().toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope", is(collection1.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", allOf(
                hasSize(1),
                hasJsonPath("$[0]._embedded.indexableObject", WorkspaceItemMatcher.matchProperties(wsi1))
            )));
    }

    /**
     * This test verifies that
     * {@link org.dspace.discovery.indexobject.InprogressSubmissionIndexFactoryImpl#storeInprogressItemFields}
     * indexes the owning collection of workflow items.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void discoverSearchObjectsTestForWorkflowItemInCollectionScope() throws Exception {
        // in-progress submissions are only visible to the person who created them
        context.setCurrentUser(eperson);

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("Community")
            .build();

        Collection collection1 = CollectionBuilder.createCollection(context, community)
            .withName("Collection 1")
            .withWorkflowGroup(1, admin) // enable the workflow, otherwise the item would be archived immediately
            .build();

        Collection collection2 = CollectionBuilder.createCollection(context, community)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin) // enable the workflow, otherwise the item would be archived immediately
            .build();

        XmlWorkflowItem wfi1 = WorkflowItemBuilder.createWorkflowItem(context, collection1)
            .withTitle("Workflow Item 1")
            .build();

        XmlWorkflowItem wfi2 = WorkflowItemBuilder.createWorkflowItem(context, collection2)
            .withTitle("Workflow Item 2")
            .build();

        context.restoreAuthSystemState();

        String ePersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(ePersonToken).perform(
                get("/api/discover/searchresults/search/objects")
                    // The workspace configuration returns all items (workspace, workflow, archived) of the current user
                    // see: https://github.com/DSpace/RestContract/blob/main/search-endpoint.md#workspace
                    .param("configuration", "workspace")
                    .param("scope", collection1.getID().toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.scope", is(collection1.getID().toString())))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", allOf(
                hasSize(1),
                hasJsonPath("$[0]._embedded.indexableObject", WorkflowItemMatcher.matchProperties(wfi1))
            )));
    }

    @Test
    public void discoverSearchObjectsTestForHitHighlights() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("AnotherTest").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        String query = "Public";
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a query stating 'public'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", query))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results has to contain the item with the query in the title and the hithighlight has
            // to be filled in with a string containing the query
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher
                    .matchOnItemNameAndHitHighlight("item", "items",
                        "Public item 2", query, "dc.title")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForHitHighlightsDisabled() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("AnotherTest").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        String query = "Public";
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a query stating 'public'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", query)
                .param("hitHightlights", "false"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results has to contain the item with the query in the title and the hithighlight has
            // to be filled in with a string containing the query
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher
                    .matchOnItemName("item", "items", "Public item 2"),
                hasJsonPath("$.hitHighlights", nullValue())
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForHitHighlightsWithPrivateItem() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
        //2. Two public items that are readable by Anonymous with different subjects and one private item
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("AnotherTest").withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        String query = "Public";
        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a query stating 'Public'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", query))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results should not contain this
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.not(
                Matchers.contains(
                    SearchResultMatcher
                        .matchOnItemNameAndHitHighlight("item", "items",
                            "Public item 2", query, "dc.title")
                ))))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorContains_query() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test*,query"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorContains() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test,contains"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotContains_query() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "-test*,query"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotContains() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test,notcontains"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForMinMaxValues() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("t, t").withAuthor("t, y")
            .withAuthor("t, r").withAuthor("t, e").withAuthor("t, z").withAuthor("t, a")
            .withAuthor("t, tq").withAuthor("t, ts").withAuthor("t, td").withAuthor("t, tf")
            .withAuthor("t, tg").withAuthor("t, th").withAuthor("t, tj").withAuthor("t, tk")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry").withSubject("a").withSubject("b").withSubject("c")
            .withSubject("d").withSubject("e").withSubject("f").withSubject("g")
            .withSubject("h").withSubject("i").withSubject("j")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("configuration" , "minAndMaxTests")
                .param("size", "2")
                .param("page", "1"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            //Size of 2 because that's what we entered
            //Page number 1 because that's the param we entered
            //TotalPages 4 because size = 2 and total elements is 7 -> 4 pages
            //We made 7 elements -> 7 total elements
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 4, 7)
            )))
            //These are the  two elements that'll be shown (because page = 1, so the third and fourth element
            // in the list) and they'll be the only ones because the size is 2
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match(),
                SearchResultMatcher.match()
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorEquals_query() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "Test,query"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorEquals() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "Test,equals"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotEquals_query() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "-Test,query"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotEquals() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "Test,notequals"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotAuthority_query() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "-id:test,query"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithQueryOperatorNotAuthority() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test,notauthority"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results have to contain the items that match the searchFilter
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithMissingQueryOperator() throws Exception {
        //** WHEN **
        // An anonymous user browses this endpoint to find the objects in the system
        // With the given search filter where there is the filter operator missing in the value (must be of form
        // <:filter-value>,<:filter-operator>)
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test"))
            //** THEN **
            //Will result in 422 status because of missing filter operator
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void discoverSearchObjectsWithNotValidQueryOperator() throws Exception {
        //** WHEN **
        // An anonymous user browses this endpoint to find the objects in the system
        // With the given search filter where there is a non-valid filter operator given (must be of form
        // <:filter-value>,<:filter-operator> where the filter operator is one of: “contains”, “notcontains”, "equals"
        // “notequals”, “authority”, “notauthority”, "query"); see enum RestSearchOperator
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("f.title", "test,operator"))
            //** THEN **
            //Will result in 422 status because of non-valid filter operator
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void discoverSearchObjectsTestWithDateIssuedQueryTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();
        String bitstreamContent = "ThisIsSomeDummyText";
        //Add a bitstream to an item
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            Bitstream bitstream = BitstreamBuilder.createBitstream(context, publicItem1, is)
                .withName("Bitstream")
                .withMimeType("text/plain")
                .build();
        }

        //Run the filter media to make the text in the bitstream searchable through the query
        runDSpaceScript("filter-media", "-f", "-i", publicItem1.getHandle());

        context.restoreAuthSystemState();

        //** WHEN **
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "dc.date.issued:\"2010-02-13\""))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            //This is the only item that should be returned with the query given
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
            )))

            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;


        //** WHEN **
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "dc.date.issued:\"2013-02-13\""))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))

            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestWithLuceneSyntaxQueryTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("TestItem2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("azeazeazeazeazeaze")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "((dc.date.issued:2010 OR dc.date.issued:1990-02-13)" +
                    " AND (dc.title:Test OR dc.title:TestItem2))"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            //This is the only item that should be returned with the query given
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                SearchResultMatcher.matchOnItemName("item", "items", "TestItem2")
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.not(Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "azeazeazeazeazeaze")
            ))))

            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestWithEscapedLuceneCharactersTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Faithful Infidel: Exploring Conformity (2nd edition)")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("NotAProperTestTitle")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "\"Faithful Infidel: Exploring Conformity (2nd edition)\""))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            //This is the only item that should be returned with the query given
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName
                    ("item", "items", "Faithful Infidel: Exploring Conformity (2nd edition)")
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.not(Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                    SearchResultMatcher.matchOnItemName("item", "items", "NotAProperTestTitle")
                ))))

            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }
    @Test
    public void discoverSearchObjectsTestWithUnEscapedLuceneCharactersTest() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
            .withTitle("Faithful Infidel: Exploring Conformity (2nd edition)")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("NotAProperTestTitle")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        context.restoreAuthSystemState();

        //** WHEN **
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("query", "OR"))

            .andExpect(status().isUnprocessableEntity())
        ;

    }

    @Test
    /**
     * This test is intended to verify that an in progress submission (workspaceitem, workflowitem, pool task and
     * claimed tasks) don't interfere with the standard search
     *
     * @throws Exception
     */
    public void discoverSearchObjectsWithInProgressSubmissionTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();
        // the second collection has a workflow active
        Collection col2 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin)
            .build();

        // 2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        //3. three in progress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
        context.setCurrentUser(eperson);
        WorkspaceItem wsItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1).withTitle("Workspace Item 1")
            .build();

        WorkspaceItem wsItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2).withTitle("Workspace Item 2")
            .build();

        XmlWorkflowItem wfItem1 = WorkflowItemBuilder.createWorkflowItem(context, col2).withTitle("Workflow Item 1")
            .build();

        // 4. a claimed task from the administrator
        ClaimedTask cTask = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Claimed Item")
            .build();

        // 5. other in progress submissions made by the administrator
        context.setCurrentUser(admin);
        WorkspaceItem wsItem1Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withTitle("Admin Workspace Item 1").build();

        WorkspaceItem wsItem2Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withTitle("Admin Workspace Item 2").build();

        XmlWorkflowItem wfItem1Admin = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withTitle("Admin Workflow Item 1").build();

        context.restoreAuthSystemState();

        //** WHEN **
        // An anonymous user, the submitter and the admin that browse this endpoint to find the public objects in the
        // system should not retrieve the in progress submissions and related objects
        String[] tokens = new String[] {
            null,
            getAuthToken(eperson.getEmail(), password),
            getAuthToken(admin.getEmail(), password),
        };

        for (String token : tokens) {
            getClient(token).perform(get("/api/discover/searchresults/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("searchresult")))
                //There needs to be a page object that shows the total pages and total elements as well as the
                // size and the current page (number)
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                    PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
                )))
                //These search results have to be shown in the embedded.objects section as these are the items
                // given in the structure defined above.
                //Seeing as everything fits onto one page, they have to all be present
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                    SearchResultMatcher.match("core", "community", "communities"),
                    SearchResultMatcher.match("core", "community", "communities"),
                    SearchResultMatcher.match("core", "item", "items"),
                    SearchResultMatcher.match("core", "item", "items"),
                    SearchResultMatcher.match("core", "item", "items"),
                    //This has to be like this because collections don't have anything else
                    // these matchers also need to be the last otherwise they will be potentially consumed for
                    // other staff
                    SearchResultMatcher.match(),
                    SearchResultMatcher.match()
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
            ;
        }
    }

    @Test
    /**
     * This test is intent to verify that workspaceitem are only visible to the submitter
     *
     * @throws Exception
     */
    public void discoverSearchObjectsWorkspaceConfigurationTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();
        // the second collection has a workflow active
        Collection col2 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin)
            .build();

        // 2. Three public items that are readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        EPerson submitter = EPersonBuilder.createEPerson(context).withEmail("submitter@example.com")
            .withPassword(password).build();
        context.setCurrentUser(submitter);
        // a public item from our submitter
        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item from submitter")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        //3. three in progress submission from our submitter user (2 ws, 1 wf that will produce also a pooltask)
        WorkspaceItem wsItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1).withTitle("Workspace Item 1")
            .withIssueDate("2010-07-23")
            .build();

        WorkspaceItem wsItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2).withTitle("Workspace Item 2")
            .withIssueDate("2010-11-03")
            .build();

        XmlWorkflowItem wfItem1 = WorkflowItemBuilder.createWorkflowItem(context, col2).withTitle("Workflow Item 1")
            .withIssueDate("2010-11-03")
            .build();

        // 4. a claimed task from the administrator
        ClaimedTask cTask = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Claimed Item")
            .withIssueDate("2010-11-03")
            .build();

        // 5. other in progress submissions made by the administrator
        context.setCurrentUser(admin);
        WorkspaceItem wsItem1Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withIssueDate("2010-07-23")
            .withTitle("Admin Workspace Item 1").build();

        WorkspaceItem wsItem2Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workspace Item 2").build();

        XmlWorkflowItem wfItem1Admin = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workflow Item 1").build();

        context.restoreAuthSystemState();
        //** WHEN **
        // each submitter, including the administrator should see only their submission
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(submitterToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workspace"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 5)
            )))
            // These search results have to be shown in the embedded.objects section
            // one public item, two workspaceitems and two worfklowitems submitted by our submitter user
            // as by the structure defined above.
            // Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("core", "item", "items"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3,
                            "Public item from submitter", "2010-02-13")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(wsItem1,
                            "Workspace Item 1", "2010-07-23")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(
                            wsItem2, "Workspace Item 2", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            wfItem1, "Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            cTask.getWorkflowItem(), "Claimed Item","2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workspace"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
            )))
            // These search results have to be shown in the embedded.objects section two workspaceitems and one
            // worfklowitem submitted by the admin user as by the structure defined above. Please note that the
            // claimedTask should be not visible here as this is the workspace configuration
            //Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(
                            wsItem1Admin, "Admin Workspace Item 1", "2010-07-23")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(
                            wsItem2Admin, "Admin Workspace Item 2", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            wfItem1Admin, "Admin Workflow Item 1", "2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;
    }

    @Test
    /**
     * This test is intent to verify that tasks are only visible to the appropriate users (reviewers)
     *
     * @throws Exception
     */
    public void discoverSearchObjectsWorkflowConfigurationTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. Two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context).withEmail("reviewer1@example.com")
            .withPassword(password).build();
        EPerson reviewer2 = EPersonBuilder.createEPerson(context).withEmail("reviewer2@example.com")
            .withPassword(password).build();

        // 2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();
        // the second collection has two workflow steps active
        Collection col2 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin, reviewer1)
            .withWorkflowGroup(2, reviewer2)
            .build();

        // 2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        //3. three in progress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
        context.setCurrentUser(eperson);
        WorkspaceItem wsItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1).withTitle("Workspace Item 1")
            .withIssueDate("2010-07-23")
            .build();

        WorkspaceItem wsItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2).withTitle("Workspace Item 2")
            .withIssueDate("2010-11-03")
            .build();

        XmlWorkflowItem wfItem1 = WorkflowItemBuilder.createWorkflowItem(context, col2).withTitle("Workflow Item 1")
            .withIssueDate("2010-11-03")
            .build();

        // 4. a claimed task from the administrator
        ClaimedTask cTask = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Claimed Item")
            .withIssueDate("2010-11-03")
            .build();

        // 5. other in progress submissions made by the administrator
        context.setCurrentUser(admin);
        WorkspaceItem wsItem1Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withIssueDate("2010-07-23")
            .withTitle("Admin Workspace Item 1").build();

        WorkspaceItem wsItem2Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workspace Item 2").build();

        XmlWorkflowItem wfItem1Admin = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workflow Item 1").build();

        // 6. a pool task in the second step of the workflow
        ClaimedTask cTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Pool Step2 Item")
            .withIssueDate("2010-11-04")
            .build();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + cTask2.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        context.restoreAuthSystemState();

        // summary of the structure, we have:
        //  a simple collection
        //  a second collection with 2 workflow steps that have 1 reviewer each (reviewer1 and reviewer2)
        //  3 public items
        //  2 workspace items submitted by a regular submitter
        //  2 workspace items submitted by the admin
        //  4 workflow items:
        //   1 pool task in step 1, submitted by the same regular submitter
        //   1 pool task in step 1, submitted by the admin
        //   1 claimed task in the first workflow step from the repository admin
        //   1 pool task in step 2, from the repository admin
        //    (This one is created by creating a claimed task for step 1 and approving it)

        //** WHEN **
        // the submitter should not see anything in the workflow configuration
        getClient(epersonToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/discover/searchresults/search/objects")))
        ;

        // reviewer1 should see two pool items, one from the submitter and one from the administrator
        // the other task in step1 is claimed by the administrator so it should be not visible to the reviewer1
        getClient(reviewer1Token).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            // These search results have to be shown in the embedded.objects section:
            // two workflow items, one submitted by the user and one submitted by the admin.
            // The claimed task of the administrator and the pool task for step 2 should not be visible to
            // reviewer1.
            // Please note that the workspace items should not be visible here either.
            // Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Admin Workflow Item 1", "2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/discover/searchresults/search/objects")))
        ;

        // admin should see two pool items and a claimed task,
        // one pool item from the submitter and one from the admin
        // because the admin is in the reviewer group for step 1, not because they are an admin
        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
            )))
            // These search results have to be shown in the embedded.objects section:
            // two workflow items and one claimed task.
            // For step 1 one submitted by the user and one submitted by the admin and none for step 2.
            //Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Admin Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "claimedtask", "claimedtask"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Claimed Item", "2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

        // reviewer2 should only see one pool item
        getClient(reviewer2Token).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            // These search results have to be shown in the embedded.objects section
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject._embedded.workflowitem",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Pool Step2 Item", "2010-11-04")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }


    @Test
    /**
     * This test is intent to verify that tasks are only visible to the appropriate users (reviewers)
     *
     * @throws Exception
     */
    public void discoverSearchObjectsWorkflowAdminConfigurationTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. Two reviewers
        EPerson reviewer1 = EPersonBuilder.createEPerson(context).withEmail("reviewer1@example.com")
            .withPassword(password).build();
        EPerson reviewer2 = EPersonBuilder.createEPerson(context).withEmail("reviewer2@example.com")
            .withPassword(password).build();

        // 2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();
        // the second collection has two workflow steps active
        Collection col2 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin, reviewer1)
            .withWorkflowGroup(2, reviewer2)
            .build();

        // 2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald").withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
            .withSubject("TestingForMore").withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
            .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest").withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        //3. three in progress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
        context.setCurrentUser(eperson);
        WorkspaceItem wsItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1).withTitle("Workspace Item 1")
            .withIssueDate("2010-07-23")
            .build();

        WorkspaceItem wsItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2).withTitle("Workspace Item 2")
            .withIssueDate("2010-11-03")
            .build();

        XmlWorkflowItem wfItem1 = WorkflowItemBuilder.createWorkflowItem(context, col2).withTitle("Workflow Item 1")
            .withIssueDate("2010-11-03")
            .build();

        // 4. a claimed task from the administrator
        ClaimedTask cTask = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Claimed Item")
            .withIssueDate("2010-11-03")
            .build();

        // 5. other in progress submissions made by the administrator
        context.setCurrentUser(admin);
        WorkspaceItem wsItem1Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withIssueDate("2010-07-23")
            .withTitle("Admin Workspace Item 1").build();

        WorkspaceItem wsItem2Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workspace Item 2").build();

        XmlWorkflowItem wfItem1Admin = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workflow Item 1").build();

        // 6. a pool task in the second step of the workflow
        ClaimedTask cTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Pool Step2 Item")
            .withIssueDate("2010-11-04")
            .build();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + cTask2.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        context.restoreAuthSystemState();

        // summary of the structure, we have:
        //  a simple collection
        //  a second collection with 2 workflow steps that have 1 reviewer each (reviewer1 and reviewer2)
        //  3 public items
        //  2 workspace items submitted by a regular submitter
        //  2 workspace items submitted by the admin
        //  4 workflow items:
        //   1 pool task in step 1, submitted by the same regular submitter
        //   1 pool task in step 1, submitted by the admin
        //   1 claimed task in the first workflow step from the repository admin
        //   1 pool task in step 2, from the repository admin
        //    (This one is created by creating a claimed task for step 1 and approving it)

        //** WHEN **
        // the submitter should not see anything in the workflow configuration
        getClient(epersonToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflowAdmin"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/discover/searchresults/search/objects")))
        ;

        // reviewer1 should not see pool items, as it is not an administrator
        getClient(reviewer1Token).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflowAdmin"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;


        // admin should see three pool items and a claimed task
        // one pool item from the submitter and two from the admin
        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflowAdmin"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 4)
            )))
            // These search results have to be shown in the embedded.objects section:
            // three workflow items and one claimed task.
            // For step 1 one submitted by the user and one submitted by the admin and one for step 2.
            //Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Admin Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Pool Step2 Item", "2010-11-04")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Claimed Item", "2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/discover/searchresults/search/objects")))
        ;

        // reviewer2 should not see pool items, as it is not an administrator
        getClient(reviewer2Token).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflowAdmin"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsTestForWithdrawnOrPrivateItemsNonAdmin() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
        //2. One public item, one private, one withdrawn.
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("WithdrawnTest 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Private Test item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("AnotherTest").withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        String query = "Test";
        //** WHEN **
        //A non-admin user browses this endpoint to find the withdrawn or private objects in the system
        //With a query stating 'Test'
        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "undiscoverable")
                .param("query", query))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results should be an empty list.
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.empty()))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))

        ;

    }

    @Test
    public void discoverSearchObjectsTestForWithdrawnOrPrivateItemsByAdminUser() throws Exception {
        //We turn off the authorization system in order to create the structure as defined below
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
        //2. One public item, one private, one withdrawn.
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Private Test item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
            .withSubject("AnotherTest").withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();
        context.restoreAuthSystemState();

        String query = "Test";
        String adminToken = getAuthToken(admin.getEmail(), password);
        //** WHEN **
        // A system admin user browses this endpoint to find the withdrawn or private objects in the system
        // With a query stating 'Test'
        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "undiscoverable")
                .param("query", query))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //The page object needs to look like this
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntry(0, 20)
            )))
            //The search results should be an empty list.
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test item 2"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test 2")
                )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")))

        ;

    }

    @Test
    public void discoverSearchObjectsTestForAdministrativeViewAnonymous() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **

        //1. A community-collection structure with one parent community with sub-community and two collections.

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder
            .createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 2")
            .build();

        //2. One public item, one private, one withdrawn.

        ItemBuilder.createItem(context, col1)
            .withTitle("Public Test Item")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test Item")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Private Test Item")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("AnotherTest")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        //** WHEN **

        // An anonymous user browses this endpoint to find the withdrawn or private objects in the system
        // With a query stating 'Test'

        getClient().perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

            //** THEN **

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item")
            )))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsTestForAdministrativeViewEPerson() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **

        //1. A community-collection structure with one parent community with sub-community and two collections.

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder
            .createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 2")
            .build();

        //2. One public item, one private, one withdrawn.

        ItemBuilder.createItem(context, col1)
            .withTitle("Public Test Item")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test Item")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Private Test Item")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("AnotherTest")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        //** WHEN **

        // A non-admin user browses this endpoint to find the withdrawn or private objects in the system
        // With a query stating 'Test'

        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

            //** THEN **

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item")
            )))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsTestForAdministrativeViewAdmin() throws Exception {

        //We turn off the authorization system in order to create the structure as defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **

        //1. A community-collection structure with one parent community with sub-community and two collections.

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder
            .createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 2")
            .build();

        //2. One public item, one private, one withdrawn.

        ItemBuilder.createItem(context, col1)
            .withTitle("Public Test Item")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test Item")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Private Test Item")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("AnotherTest")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        //** WHEN **

        // A system admin user browses this endpoint to find the withdrawn or private objects in the system
        // With a query stating 'Test'

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

            //** THEN **

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsTestForAdministrativeViewWithFilters() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder
            .createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 2")
            .build();

        ItemBuilder.createItem(context, col1)
            .withTitle("Public Test Item")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test Item")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Private Test Item")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("AnotherTest")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.withdrawn", "true,contains")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.contains(
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.withdrawn", "false,contains")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.discoverable", "true,contains")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.discoverable", "false,contains")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.contains(
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsTestForAdministrativeViewWithFiltersEquals() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder
            .createCommunity(context)
            .withName("Parent Community")
            .build();
        Community child1 = CommunityBuilder
            .createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();
        Collection col1 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 1")
            .build();
        Collection col2 = CollectionBuilder
            .createCollection(context, child1)
            .withName("Collection 2")
            .build();

        ItemBuilder.createItem(context, col1)
            .withTitle("Public Test Item")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Withdrawn Test Item")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("ExtraEntry")
            .withdrawn()
            .build();

        ItemBuilder.createItem(context, col2)
            .withTitle("Private Test Item")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withSubject("AnotherTest")
            .withSubject("ExtraEntry")
            .makeUnDiscoverable()
            .build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.withdrawn", "true,equals")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.contains(
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.withdrawn", "false,equals")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.discoverable", "true,equals")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.containsInAnyOrder(
                    SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                    SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test")
                .param("f.discoverable", "false,equals")
            )

            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("searchresult")))
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                Matchers.contains(
                    SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                )
            ))
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchPoolTaskObjectsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        EPerson reviewer = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
            .withPassword(password).build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withWorkflowGroup(1, reviewer, admin).build();

        ItemBuilder.createItem(context, col)
            .withTitle("Punnett square")
            .withIssueDate("2016-02-13")
            .withAuthor("Bandola, Roman")
            .withSubject("ExtraEntry").build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Metaphysics")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry").build();

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Mathematical Theory")
            .withIssueDate("2020-01-19")
            .withAuthor("Tommaso, Gattari")
            .withSubject("ExtraEntry").build();

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Test Metaphysics")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow")
                .param("sort", "dc.date.issued,DESC")
                .param("query", "Mathematical Theory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query", is("Mathematical Theory")))
            .andExpect(jsonPath("$.configuration", is("workflow")))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks")
            )))
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)));

        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow")
                .param("sort", "dc.date.issued,DESC")
                .param("query", "Metaphysics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query", is("Metaphysics")))
            .andExpect(jsonPath("$.configuration", is("workflow")))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks")
            )))
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(2)));
    }

    @Test
    public void discoverSearchPoolTaskObjectsEmptyQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        EPerson reviewer = EPersonBuilder.createEPerson(context)
            .withEmail("reviewer1@example.com")
            .withPassword(password).build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .withWorkflowGroup(1, reviewer, admin).build();

        ItemBuilder.createItem(context, col)
            .withTitle("Punnett square")
            .withIssueDate("2016-02-13")
            .withAuthor("Bandola, Roman")
            .withSubject("ExtraEntry").build();

        // create a normal user to use as submitter
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password).build();

        context.setCurrentUser(submitter);

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Metaphysics")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry").build();

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Mathematical Theory")
            .withIssueDate("2020-01-19")
            .withAuthor("Tommaso, Gattari")
            .withSubject("ExtraEntry").build();

        PoolTaskBuilder.createPoolTask(context, col, reviewer)
            .withTitle("Test Metaphysics")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry").build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/discover/searchresults/search/objects")
                .param("configuration", "workflow")
                .param("sort", "dc.date.issued,DESC")
                .param("query", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configuration", is("workflow")))
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks"),
                SearchResultMatcher.match("workflow", "pooltask", "pooltasks")
            )))
            .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(3)));
    }

    @Test
    /**
     * This test is intent to verify that tasks are only visible to the admin users
     *
     * @throws Exception
     */
    public void discoverSearchObjectsSupervisionConfigurationTest() throws Exception {

        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        // 1. Two reviewers and two users and two groups
        EPerson reviewer1 =
            EPersonBuilder.createEPerson(context)
                .withEmail("reviewer1@example.com")
                .withPassword(password)
                .build();

        EPerson reviewer2 =
            EPersonBuilder.createEPerson(context)
                .withEmail("reviewer2@example.com")
                .withPassword(password)
                .build();

        EPerson userA =
            EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withEmail("userA@test.com")
                .withPassword(password)
                .build();

        EPerson userB =
            EPersonBuilder.createEPerson(context)
                .withCanLogin(true)
                .withEmail("userB@test.com")
                .withPassword(password)
                .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                .withName("group A")
                .addMember(userA)
                .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                .withName("group B")
                .addMember(userB)
                .build();

        // 2. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
            .withName("Sub Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 1")
            .build();

        // the second collection has two workflow steps active
        Collection col2 = CollectionBuilder.createCollection(context, child1)
            .withName("Collection 2")
            .withWorkflowGroup(1, admin, reviewer1)
            .withWorkflowGroup(2, reviewer2)
            .build();

        // 2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withAuthor("Testing, Works")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
            .withTitle("Test 2")
            .withIssueDate("1990-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withAuthor("Testing, Works")
            .withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
            .withTitle("Public item 2")
            .withIssueDate("2010-02-13")
            .withAuthor("Smith, Maria")
            .withAuthor("Doe, Jane")
            .withAuthor("test,test")
            .withAuthor("test2, test2")
            .withAuthor("Maybe, Maybe")
            .withSubject("AnotherTest")
            .withSubject("TestingForMore")
            .withSubject("ExtraEntry")
            .build();

        //3. three in progress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
        context.setCurrentUser(eperson);
        WorkspaceItem wsItem1 = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withTitle("Workspace Item 1")
            .withIssueDate("2010-07-23")
            .build();

        WorkspaceItem wsItem2 = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withTitle("Workspace Item 2")
            .withIssueDate("2010-11-03")
            .build();

        XmlWorkflowItem wfItem1 = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withTitle("Workflow Item 1")
            .withIssueDate("2010-11-03")
            .build();

        // create Four supervision orders for above items
        SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1.getItem(), groupA).build();

        SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2.getItem(), groupA).build();

        SupervisionOrderBuilder.createSupervisionOrder(context, wfItem1.getItem(), groupA).build();
        SupervisionOrderBuilder.createSupervisionOrder(context, wfItem1.getItem(), groupB).build();

        // 4. a claimed task from the administrator
        ClaimedTask cTask = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Claimed Item")
            .withIssueDate("2010-11-03")
            .build();

        // 5. other in progress submissions made by the administrator
        context.setCurrentUser(admin);
        WorkspaceItem wsItem1Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col1)
            .withIssueDate("2010-07-23")
            .withTitle("Admin Workspace Item 1").build();

        WorkspaceItem wsItem2Admin = WorkspaceItemBuilder.createWorkspaceItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workspace Item 2").build();

        XmlWorkflowItem wfItem1Admin = WorkflowItemBuilder.createWorkflowItem(context, col2)
            .withIssueDate("2010-11-03")
            .withTitle("Admin Workflow Item 1").build();

        // create Four supervision orders for above items
        SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1Admin.getItem(), groupA).build();

        SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2Admin.getItem(), groupA).build();

        SupervisionOrderBuilder.createSupervisionOrder(context, wfItem1Admin.getItem(), groupA).build();
        SupervisionOrderBuilder.createSupervisionOrder(context, wfItem1Admin.getItem(), groupB).build();

        // 6. a pool task in the second step of the workflow
        ClaimedTask cTask2 = ClaimedTaskBuilder.createClaimedTask(context, col2, admin).withTitle("Pool Step2 Item")
            .withIssueDate("2010-11-04")
            .build();

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);
        String reviewer1Token = getAuthToken(reviewer1.getEmail(), password);
        String reviewer2Token = getAuthToken(reviewer2.getEmail(), password);

        getClient(adminToken).perform(post("/api/workflow/claimedtasks/" + cTask2.getID())
                .param("submit_approve", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(status().isNoContent());

        context.restoreAuthSystemState();

        // summary of the structure, we have:
        //  a simple collection
        //  a second collection with 2 workflow steps that have 1 reviewer each (reviewer1 and reviewer2)
        //  3 public items
        //  2 workspace items submitted by a regular submitter
        //  2 workspace items submitted by the admin
        //  4 workflow items:
        //   1 pool task in step 1, submitted by the same regular submitter
        //   1 pool task in step 1, submitted by the admin
        //   1 claimed task in the first workflow step from the repository admin
        //   1 pool task task in step 2, from the repository admin
        //    (This one is created by creating a claimed task for step 1 and approving it)

        //** WHEN **
        // the submitter should not see anything in the workflow configuration
        getClient(epersonToken)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "supervision"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        // reviewer1 should not see pool items, as it is not an administrator
        getClient(reviewer1Token)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "supervision"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/discover/searchresults/search/objects")));

        // admin should see seven pool items and a claimed task
        // Three pool items from the submitter and Five from the admin
        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "supervision"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 8)
            )))
            // These search results have to be shown in the embedded.objects section:
            // three workflow items and one claimed task.
            // For step 1 one submitted by the user and one submitted by the admin and one for step 2.
            //Seeing as everything fits onto one page, they have to all be present
            .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Admin Workflow Item 1", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Pool Step2 Item", "2010-11-04")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("workflow", "workflowitem", "workflowitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkflowItemMatcher.matchItemWithTitleAndDateIssued(
                            null, "Claimed Item", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(wsItem1,
                            "Workspace Item 1", "2010-07-23")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(wsItem2,
                            "Workspace Item 2", "2010-11-03")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(wsItem1Admin,
                            "Admin Workspace Item 1", "2010-07-23")))
                ),
                Matchers.allOf(
                    SearchResultMatcher.match("submission", "workspaceitem", "workspaceitems"),
                    JsonPathMatchers.hasJsonPath("$._embedded.indexableObject",
                        is(WorkspaceItemMatcher.matchItemWithTitleAndDateIssued(wsItem2Admin,
                            "Admin Workspace Item 2", "2010-11-03")))
                )
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));

        // reviewer2 should not see pool items, as it is not an administrator
        getClient(reviewer2Token)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "supervision"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsNOTIFYIncomingConfigurationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("item title")
            .build();

        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                .withDescription("service description")
                .withUrl("service url")
                .withLdnUrl("https://overlay-journal.com/inbox/")
                .build();

        InputStream announceEndorsementStream = getClass().getResourceAsStream("ldn_announce_endorsement.json");
        String announceEndorsement = IOUtils.toString(announceEndorsementStream, Charset.defaultCharset());
        announceEndorsementStream.close();

        String message = announceEndorsement.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);
        Notification notification = mapper.readValue(message, Notification.class);
        LDNMessageBuilder.createNotifyServiceBuilder(context, notification).build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "NOTIFY.incoming"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }

    @Test
    public void discoverSearchObjectsNOTIFYOutgoingConfigurationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("item title")
            .build();

        String object = configurationService.getProperty("dspace.ui.url") + "/handle/" + item.getHandle();

        NotifyServiceEntity notifyService =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                .withDescription("service description")
                .withUrl("service url")
                .withLdnUrl("https://generic-service.com/system/inbox/")
                .build();

        InputStream announceReviewStream = getClass().getResourceAsStream("ldn_announce_review.json");
        String announceReview = IOUtils.toString(announceReviewStream, Charset.defaultCharset());
        announceReviewStream.close();


        String message = announceReview.replaceAll("<<object>>", object);
        message = message.replaceAll("<<object_handle>>", object);
        Notification notification = mapper.readValue(message, Notification.class);
        LDNMessageBuilder.createNotifyServiceBuilder(context, notification).build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
            .perform(get("/api/discover/searchresults/search/objects").param("configuration", "NOTIFY.outgoing"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("searchresult")))
            //There needs to be a page object that shows the total pages and total elements as well as the
            // size and the current page (number)
            .andExpect(jsonPath("$._embedded.searchResult.page", is(
                PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
            )))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/searchresults/search/objects")));
    }
}
