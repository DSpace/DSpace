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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.AppliedFilterMatcher;
import org.dspace.app.rest.matcher.FacetEntryMatcher;
import org.dspace.app.rest.matcher.FacetValueMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.SearchFilterMatcher;
import org.dspace.app.rest.matcher.SearchResultMatcher;
import org.dspace.app.rest.matcher.SortOptionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Test;

public class DiscoveryRestControllerIT extends AbstractControllerIntegrationTest {


    @Test
    public void rootDiscoverTest() throws Exception {

        //When we call this endpoint
        getClient().perform(get("/api/discover"))

                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a link to the facets endpoint
                .andExpect(jsonPath("$._links.facets.href", containsString("api/discover/facets")))
                //There needs to be a link to the search endpoint
                .andExpect(jsonPath("$._links.search.href", containsString("api/discover/search")))
                //There needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover")));
    }

    @Test
    public void discoverFacetsTestWithoutParameters() throws Exception{

        //When we call this facets endpoint
        getClient().perform(get("/api/discover/facets"))

                //We expect a 200 OK status
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a self link to this endpoint
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
                //We have 4 facets in the default configuration, they need to all be present in the embedded section
                .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(),
                        FacetEntryMatcher.dateIssuedFacet(),
                        FacetEntryMatcher.subjectFacet(),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet()))
                );
    }

    @Test
    public void discoverFacetsAuthorTestWithSizeParameter() throws Exception{
        //Turn off the authorization system, otherwise we can't make the objects
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

        //2. Three public items that are readable by Anonymous with different subjects and authors
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 2
        getClient().perform(get("/api/discover/facets/author")
                    .param("size", "2"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name of the facet needs to be author, because that's what we called
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' because that's how the author facet is configured by default
                .andExpect(jsonPath("$.facetType", is("text")))
                //Because we've constructed such a structure so that we have more than 2 (size) authors, there needs to be a next link
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/author?page")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //Because there are more authors than is represented (because of the size param), hasMore has to be true
                .andExpect(jsonPath("$.hasMore", is(Boolean.TRUE)))
                //The page object needs to be present and just like specified in the matcher
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,2))))
                //These authors need to be in the response because it's sorted on how many times the author comes up in different items
                //These authors are the most used ones. Only two show up because of the size.
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria")
                )))
        ;
    }

    @Test
    public void discoverFacetsAuthorTestForHasMoreFalse() throws Exception{
        //Turn of the authorization system so that we can create the structure specified below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the authors by the facets and doesn't enter a size
        getClient().perform(get("/api/discover/facets/author"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author, because that's the facet that we called upon
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' because that's the default configuration for this facet
                .andExpect(jsonPath("$.facetType", is("text")))
                //There always needs to be a self link present
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //hasMore has to be false because we've not entered more than 20 (this is default size) authors
                .andExpect(jsonPath("$.hasMore", is(Boolean.FALSE)))
                //The page object needs to present and exactly like how it is specified here. 20 is entered as the size because that's the default in the configuration if no size parameter has been given
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,20))))
                //The authors need to be embedded in the values, all 4 of them have to be present as the size allows it
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria"),
                        FacetValueMatcher.entryAuthor("Doe, John"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )))
        ;
    }

    @Test
    public void discoverFacetsAuthorTestForPagination() throws Exception{

        //We turn off the authorization system in order to construct the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Doe, John").withAuthor("Smith, Donald")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the authors by the facet
        //The user enters a size of two and wants to see page 1, this is the second page.
        getClient().perform(get("/api/discover/facets/author")
                .param("size", "2")
                .param("page", "1"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name of the facet has to be author as that's the one we called
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' as this is the default configuration
                .andExpect(jsonPath("$.facetType", is("text")))
                //There needs to be a next link because there are more authors than the current size is allowed to show. There are more pages after this one
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/author?page")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //hasMore needs to be true because there are more elements than currently shown, there are additional pages
                .andExpect(jsonPath("$.hasMore", is(Boolean.TRUE)))
                //The page object has to be like this because that's what we've asked in the parameters
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(1,2))))
                //These authors have to be present because of the current configuration
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, John"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )))
        ;
    }


    @Test
    public void discoverFacetsTestWithSimpleQueryAndSearchFilter() throws Exception{
        //Turn off the authorization system to construct the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        //** WHEN **
        //An anonymous user browses this endpoint to find the authors by the facet
        //The user enters a small query, namely the title has to contain 'test'
        getClient().perform(get("/api/discover/facets/author")
                    .param("f.title", "test,contains"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author as that's the facet that we've asked
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType needs to be 'text' as that's the default configuration for the given facet
                .andExpect(jsonPath("$.facetType", is("text")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //The self link needs to contain the query that was specified in the parameters, this is how it looks like
                .andExpect(jsonPath("$._links.self.href", containsString("f.title=test,contains")))
                //The hasMore property has to be false because we don't have more than 20 authors that take part in the results
                .andExpect(jsonPath("$.hasMore", is(Boolean.FALSE)))
                //The applied filters have to be specified like this, applied filters are the parameters given below starting with f.
                .andExpect(jsonPath("$.appliedFilters", containsInAnyOrder(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //This is how the page object must look like because it's the default
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,20))))
                //These authors need to be present in the result because these have made items that contain 'test' in the title
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Smith, Donald"),
                        FacetValueMatcher.entryAuthor("Testing, Works")
                )))
                //These authors cannot be present because they've not produced an item with 'test' in the title
                .andExpect(jsonPath("$._embedded.values", not(containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Smith, Maria"),
                        FacetValueMatcher.entryAuthor("Doe, Jane")
                ))))
        ;
    }

    @Test
    public void discoverFacetsDateTest() throws Exception{
        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2000-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        //** WHEN **
        //An anonymous user browses this endpoint to find the dateIssued results by the facet
        getClient().perform(get("/api/discover/facets/dateIssued"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be 'dateIssued' as that's the facet that we've called
                .andExpect(jsonPath("$.name", is("dateIssued")))
                //The facetType has to be 'date' because that's the default configuration for this facet
                .andExpect(jsonPath("$.facetType", is("date")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
                //The hasMore property has to be set on false as we've not gotten more results than we're able to show
                .andExpect(jsonPath("$.hasMore", is(Boolean.FALSE)))
                //This is how the page object must look like because it's the default with size 20
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,20))))
                //The date values need to be as specified below
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        //We'll always get atleast two intervals with the items specified above, so we ask to match twice atleast
                        FacetValueMatcher.entryDateIssued(),
                        FacetValueMatcher.entryDateIssued()
                )))
        ;
    }



    @Test
    public void discoverFacetsTestWithScope() throws Exception{
        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the author results by the facet
        //With a certain scope
        getClient().perform(get("/api/discover/facets/author")
                    .param("scope", "testScope"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author as that's the facet that we've called
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' as that's the default configuration for this facet
                .andExpect(jsonPath("$.facetType", is("text")))
                //The scope has to be the same as the one that we've given in the parameters
                .andExpect(jsonPath("$.scope", is("testScope")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //the hasMore property needs to be false as we're currently able to show all the authors on one page
                .andExpect(jsonPath("$.hasMore", is(Boolean.FALSE)))
                //These are all the authors for the items that were created and thus they have to be present in the embedded values section
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria"),
                        FacetValueMatcher.entryAuthor("Doe, John"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )));
        //** WHEN **
        //An anonymous user browses this endpoint to find the author results by the facet
        //With a certain scope
        //And a size of 2
        getClient().perform(get("/api/discover/facets/author")
                    .param("scope", "testScope")
                    .param("size", "2"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be 'author' as that's the facet that we called
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' as that's the default configuration for this facet
                .andExpect(jsonPath("$.facetType", is("text")))
                //The scope has to be same as the param that we've entered
                .andExpect(jsonPath("$.scope", is("testScope")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //The hasMore property needs to be true because there are more values than that we're able to show on a page of size 2
                .andExpect(jsonPath("$.hasMore", is(Boolean.TRUE)))
                //These are the values that need to be present as it's ordered by count and these authors are the most common ones in the items that we've created
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria")
                )))
        ;
    }

    @Test
    public void discoverFacetsDateTestForHasMore() throws Exception{
        //We turn off the authorization system to create the structure defined below
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

        //2. 7 public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1940-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem4 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1950-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem5 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1960-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem6 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1970-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem7 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1980-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the dateIssued results by the facet
        //And a size of 2
        getClient().perform(get("/api/discover/facets/dateIssued")
                .param("size", "2"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name needs to be dateIssued as that's the facet that we've called
                .andExpect(jsonPath("$.name", is("dateIssued")))
                //the facetType needs to be 'date' as that's the default facetType for this facet in the configuration
                .andExpect(jsonPath("$.facetType", is("date")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
                //Seeing as we've entered a size of two and there are more dates than just two, we'll need a next link to go to the next page to see the rest of the dates
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/dateIssued?page")))
                //The hasMore property needs to be true for the same reason as the next link
                .andExpect(jsonPath("$.hasMore", is(Boolean.TRUE)))
                //The page object needs to look like this because we've entered a size of 2 and we didn't specify a starting page so it defaults to 0
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,2))))
                //There needs to be two date results in the embedded values section because that's what we've specified
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryDateIssued(),
                        FacetValueMatcher.entryDateIssued()
                )))
        ;
    }


    @Test
    public void discoverFacetsDateTestWithSearchFilter() throws Exception{

        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        //** WHEN **
        //An anonymous user browses this endpoint to find the dateIssued results by the facet
        //With a query stating that the title needs to contain 'test'
        //And a size of 2
        getClient().perform(get("/api/discover/facets/dateIssued")
                .param("f.title", "test,contains")
                .param("size", "2"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be dateIssued because that's the facet that we called
                .andExpect(jsonPath("$.name", is("dateIssued")))
                //The facetType needs to be 'date' as that's the default facetType for this facet in the configuration
                .andExpect(jsonPath("$.facetType", is("date")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
                //The hasMore property needs to be false because all the dates are able to show in two intervals, therefore we don't need more pages
                .andExpect(jsonPath("$.hasMore", is(Boolean.FALSE)))
                //There needs to be an appliedFilters section that looks like this because we've specified a query in the parameters
                .andExpect(jsonPath("$.appliedFilters", containsInAnyOrder(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //The page object needs to look like this because we entered a size of 2 and we didn't specify a starting page so it defaults to 0
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0,2))))
                //There needs to be only two date intervals with a count of 1 because of the query we specified
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryDateIssuedWithCountOne(),
                        FacetValueMatcher.entryDateIssuedWithCountOne()
                )))
        ;
    }


    @Test
    public void discoverSearchTest() throws Exception{

        //When calling this root endpoint
        getClient().perform(get("/api/discover/search"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a link to the objects that contains a string as specified below
                .andExpect(jsonPath("$._links.objects.href", containsString("api/discover/search/objects")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search")))
                //There needs to be a section where these filters as specified as they're the default filters given in the configuration
                .andExpect(jsonPath("$.filters", containsInAnyOrder(
                        SearchFilterMatcher.titleFilter(),
                        SearchFilterMatcher.authorFilter(),
                        SearchFilterMatcher.subjectFilter(),
                        SearchFilterMatcher.dateIssuedFilter(),
                        SearchFilterMatcher.hasContentInOriginalBundleFilter()
                )))
                //These sortOptions need to be present as it's the default in the configuration
                .andExpect(jsonPath("$.sortOptions", containsInAnyOrder(
                        SortOptionMatcher.titleSortOption(),
                        SortOptionMatcher.dateIssuedSortOption()
                )));
    }

    @Test
    public void discoverSearchObjectsTest() throws Exception{

        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        getClient().perform(get("/api/discover/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a page object that shows the total pages and total elements as well as the size and the current page (number)
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 7)
                )))
                //These search results have to be shown in the embedded.searchResults section as these are the items given in the structure defined above.
                //Seeing as everything fits onto one page, they have to all be present
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("community", "communities"),
                        SearchResultMatcher.match("community", "communities"),
                        //This has to be like this because collections don't have anything else
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestHasMoreAuthorFacet() throws Exception{
        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works").withAuthor("a1, a1").withAuthor("b, b").withAuthor("c, c").withAuthor("d, d").withAuthor("e, e").withAuthor("f, f").withAuthor("g, g")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system
        getClient().perform(get("/api/discover/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because we've only made 7 elements, the default size is 20 and they all fit onto one page (20 > 7) so totalPages has to be 1. Number is 0 because
                //page 0 is the default page we view if not specified otherwise
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 7)
                )))
                //All elements have to be present in the embedded.searchResults section, these are the ones we made in the structure defined above
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("community", "communities"),
                        SearchResultMatcher.match("community", "communities"),
                        //Match without any parameters because collections don't have anything special to check in the json
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                //We do however exceed the limit for the authors, so this property has to be true for the author facet
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(true),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }


    @Test
    public void discoverSearchObjectsTestHasMoreSubjectFacet() throws Exception{
        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry").withSubject("a")
                .withSubject("b").withSubject("c")
                .withSubject("d").withSubject("e")
                .withSubject("f").withSubject("g")
                .withSubject("h").withSubject("i")
                .withSubject("j").withSubject("k")

                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        getClient().perform(get("/api/discover/search/objects"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because we've only made 7 items, they all fit onto 1 page because the default size is 20 and the default starting page is 0.
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 7)
                )))
                //All the elements created in the structure above have to be present in the embedded.searchResults section
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("community", "communities"),
                        SearchResultMatcher.match("community", "communities"),
                        //Collections are specified like this because they don't have any special properties
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                //We do however exceed the limit for the subject, so this property has to be true for the subject facet
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(true),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestWithBasicQuery() throws Exception{
        //We turn off the authorization system in order to create the structure defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a query that says that the title has to contain 'test'
        getClient().perform(get("/api/discover/search/objects")
                    .param("f.title", "test,contains"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because of the query we specified, only two elements match the query.
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 2)
                )))
                //Only the two item elements match the query, therefore those are the only ones that can be in the embedded.searchResults section
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //We need to display the appliedFilters object that contains the query that we've ran
                .andExpect(jsonPath("$.appliedFilters", containsInAnyOrder(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }


    @Test
    public void discoverSearchObjectsTestWithScope() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a scope 'test'
        getClient().perform(get("/api/discover/search/objects")
                .param("scope", "test"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page element has to look like this because it contains all the elements we've just created
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 7)
                )))
                //The scope property has to be set to the value we entered in the parameters
                .andExpect(jsonPath("$.scope", is("test")))
                //All the elements created in the structure above have to be present in the embedded.searchResults section
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("community", "communities"),
                        SearchResultMatcher.match("community", "communities"),
                        //Collections are specified like this because they don't have any special properties
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestWithDsoType() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a dsoType 'item'
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "item"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page element needs to look like this and only have three totalElements because we only want the items (dsoType) and we only created three items
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 3)
                )))
                //Only the three items can be present in the embedded.searchResults section as that's what we specified in the dsoType parameter
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }

    @Test
    public void discoverSearchObjectsTestWithDsoTypeAndSort() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Testing")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a dsoType 'item'
        //And a sort on the dc.title ascending
        getClient().perform(get("/api/discover/search/objects")
                    .param("dsoType", "item")
                    .param("sort", "dc.title,ASC"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this and only contain three total elements because we only want to get the items back
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0,20, 1, 3)
                )))
                //Only the three items can be present in the embedded.searchResults section as that's what we specified in the dsoType parameter
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items"),
                        SearchResultMatcher.match("item", "items")
                )))
                //Here we want to match on the item name in a certain specified order because we want to check the sort properly
                //We check whether the items are sorted properly as we expected
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.contains(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Testing")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //We want to get the sort that's been used as well in the response
                .andExpect(jsonPath("$.sort", is(
                        SortOptionMatcher.sortByAndOrder("dc.title", "ASC")
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }


    @Test
    public void discoverFacetsDateTestWithLabels() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. 9 public items that are readable by Anonymous with different subjects
        Item publicItem6 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("2017-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem7 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem8 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem9 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1970-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem10 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1950-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem11 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1930-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem12 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1910-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem13 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1890-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();
        Item publicItem14 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1866-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find dateIssued facet values
        getClient().perform(get("/api/discover/facets/dateIssued"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because the default size is 20 and the default starting page is 0
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntry(0,20)
                )))
                //Then we expect these dateIssued values to be present with the labels as specified
                .andExpect(jsonPath("$._embedded.values", Matchers.containsInAnyOrder(
                        FacetValueMatcher.entryDateIssuedWithLabel("2000 - 2017"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1980 - 1999"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1960 - 1979"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1940 - 1959"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1920 - 1939"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1880 - 1899"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1866 - 1879"),
                        FacetValueMatcher.entryDateIssuedWithLabel("1900 - 1919")

                )))
        ;
    }

    @Test
    public void discoverSearchObjectsTestForPagination() throws Exception{
        //We turn off the authorization system in order to create the structure as defined below
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

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = new ItemBuilder().createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = new ItemBuilder().createItem(context, col2)
                .withTitle("Test 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = new ItemBuilder().createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test").withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/search/objects")
                    .param("size", "2")
                    .param("page", "1"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                //Size of 2 because that's what we entered
                //Page number 1 because that's the param we entered
                //TotalPages 4 because size = 2 and total elements is 7 -> 4 pages
                //We made 7 elements -> 7 total elements
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(1,2, 4, 7)
                )))
                //These are the  two elements that'll be shown (because page = 1, so the third and fourth element in the list) and they'll be the only ones because the size is 2
                .andExpect(jsonPath("$._embedded.searchResults", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match()
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetInSearchObject(false),
                        FacetEntryMatcher.subjectFacetInSearchObject(false),
                        FacetEntryMatcher.dateIssuedFacetInSearchObject(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacetInSearchObjects(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }

}
