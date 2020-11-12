/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.AppliedFilterMatcher;
import org.dspace.app.rest.matcher.FacetEntryMatcher;
import org.dspace.app.rest.matcher.FacetValueMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.SearchResultMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class DiscoveryRelatedBoxComponentIT extends AbstractControllerIntegrationTest {

    private String uuidFirstPerson;
    private String uuidSecondPerson;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        //Turn off the authorization system to construct the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection colPeople = CollectionBuilder.createCollection(context, child1).withName("Collection 1")
                .withRelationshipType("Person").build();
        Collection colPub = CollectionBuilder.createCollection(context, child1).withName("Collection 2")
                .withRelationshipType("Publication").build();

        Item firstPerson = createItem(context, colPeople)
                .withRelationshipType("Person")
                .withTitle("Smith, Donald")
                .build();

        Item secondPerson = createItem(context, colPeople)
                .withRelationshipType("Person")
                .withTitle("Smith, Maria")
                .build();

        uuidFirstPerson = firstPerson.getID().toString();
        uuidSecondPerson = secondPerson.getID().toString();

        Item publicItem1 = ItemBuilder.createItem(context, colPub)
                .withTitle("Test item")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald", uuidFirstPerson)
                .withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, colPub)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria", uuidSecondPerson)
                .withAuthor("Doe, Jane")
                .withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, colPub)
                .withTitle("Another Public item")
                .withIssueDate("2018-12-13")
                .withAuthor("Smith, M.", uuidSecondPerson)
                .withAuthor("Doe, Jane")
                .withAuthor("test,test")
                .withAuthor("test2, test2")
                .withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem4 = ItemBuilder.createItem(context, colPub)
                .withTitle("Testing with multiple person profiles involved")
                .withIssueDate("2020-10-23")
                .withAuthor("Smith, Maria", uuidSecondPerson)
                .withAuthor("Doe, Jane")
                .withAuthor("Testing, Works")
                .withAuthor("Smith, Donald", uuidFirstPerson)
                .withSubject("Multiple").withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void discoverFacetsInRelatedBoxComponent() throws Exception {
        //** WHEN **
        //An anonymous user browses this endpoint to find the authors by the facet
        //The user enters a small query, namely the title has to contain 'test'
        getClient().perform(get("/api/discover/facets/subject")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidFirstPerson))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author as that's the facet that we've asked
                .andExpect(jsonPath("$.name", is("subject")))
                //The facetType needs to be 'text' as that's the default configuration for the given facet
                .andExpect(jsonPath("$.facetType", is("hierarchical")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/subject")))
                //The self link needs to contain the query that was specified in the parameters, this is how it
                // looks like
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidFirstPerson)))
                //This is how the page object must look like because it's the default
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //These subject need to be present in the result
                .andExpect(jsonPath("$._embedded.values", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryText("subject", "ExtraEntry", 2),
                        FacetValueMatcher.entryText("subject", "Multiple", 1)
                )));
        // verify that filter are used
        getClient().perform(get("/api/discover/facets/subject")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidFirstPerson)
                .param("f.title", "item,contains"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author as that's the facet that we've asked
                .andExpect(jsonPath("$.name", is("subject")))
                //The facetType needs to be 'text' as that's the default configuration for the given facet
                .andExpect(jsonPath("$.facetType", is("hierarchical")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/subject")))
                //The self link needs to contain the query that was specified in the parameters, this is how it
                // looks like
                .andExpect(jsonPath("$._links.self.href", containsString("f.title=item,contains")))
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidFirstPerson)))
                //The applied filters have to be specified like this, applied filters are the parameters given
                // below starting with f.
                .andExpect(jsonPath("$.appliedFilters", contains(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "item", "item")
                )))
                //This is how the page object must look like because it's the default
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //These subject need to be present in the result
                .andExpect(jsonPath("$._embedded.values", Matchers.hasSize(1)))
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryText("subject", "ExtraEntry", 1)
                )));
        getClient().perform(get("/api/discover/facets/subject")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidSecondPerson))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name has to be author as that's the facet that we've asked
                .andExpect(jsonPath("$.name", is("subject")))
                //The facetType needs to be 'text' as that's the default configuration for the given facet
                .andExpect(jsonPath("$.facetType", is("hierarchical")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/subject")))
                //The self link needs to contain the query that was specified in the parameters, this is how it
                // looks like
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidSecondPerson)))
                //This is how the page object must look like because it's the default
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //These subject need to be present in the result
                .andExpect(jsonPath("$._embedded.values", Matchers.hasSize(4)))
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryText("subject", "ExtraEntry", 3),
                        FacetValueMatcher.entryText("subject", "TestingForMore", 2),
                        FacetValueMatcher.entryText("subject", "AnotherTest", 1),
                        FacetValueMatcher.entryText("subject", "Multiple", 1)
                )));
    }

    @Test
    public void discoverSearchObjectsTestWithScope() throws Exception {
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a scope 'test'
        getClient().perform(get("/api/discover/search/objects")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidFirstPerson))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page element has to look like this because it contains all the elements we've just created
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
                )))
                //The scope property has to be set to the value we entered in the parameters
                .andExpect(jsonPath("$.scope", is(uuidFirstPerson)))
                .andExpect(jsonPath("$.configuration", is("RELATION.Person.researchoutputs")))
                //All the elements created in the structure above have to be present in the embedded.objects section
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test item" ),
                        SearchResultMatcher.matchOnItemName("item", "items",
                                "Testing with multiple person profiles involved")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.anyFacet("chart.pie.itemtype_filter", "chart.pie"),
                        FacetEntryMatcher.anyFacet("chart.bar.dateIssued.year", "chart.bar"),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.anyFacet("editor", "text"),
                        FacetEntryMatcher.anyFacet("organization", "text"),
                        FacetEntryMatcher.anyFacet("funding", "text"),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidFirstPerson)))
        ;
        getClient().perform(get("/api/discover/search/objects")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidFirstPerson)
                .param("f.title", "item,contains"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page element has to look like this because it contains all the elements we've just created
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                //The scope property has to be set to the value we entered in the parameters
                .andExpect(jsonPath("$.scope", is(uuidFirstPerson)))
                .andExpect(jsonPath("$.configuration", is("RELATION.Person.researchoutputs")))
                //All the elements created in the structure above have to be present in the embedded.objects section
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test item" )
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.anyFacet("chart.pie.itemtype_filter", "chart.pie"),
                        FacetEntryMatcher.anyFacet("chart.bar.dateIssued.year", "chart.bar"),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.anyFacet("editor", "text"),
                        FacetEntryMatcher.anyFacet("organization", "text"),
                        FacetEntryMatcher.anyFacet("funding", "text"),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidFirstPerson)))
        ;
        getClient().perform(get("/api/discover/search/objects")
                .param("configuration", "RELATION.Person.researchoutputs")
                .param("scope", uuidSecondPerson))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page element has to look like this because it contains all the elements we've just created
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)
                )))
                //The scope property has to be set to the value we entered in the parameters
                .andExpect(jsonPath("$.scope", is(uuidSecondPerson)))
                .andExpect(jsonPath("$.configuration", is("RELATION.Person.researchoutputs")))
                //All the elements created in the structure above have to be present in the embedded.objects section
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2" ),
                        SearchResultMatcher.matchOnItemName("item", "items", "Another Public item" ),
                        SearchResultMatcher.matchOnItemName("item", "items",
                                "Testing with multiple person profiles involved")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.anyFacet("chart.pie.itemtype_filter", "chart.pie"),
                        FacetEntryMatcher.anyFacet("chart.bar.dateIssued.year", "chart.bar"),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.anyFacet("editor", "text"),
                        FacetEntryMatcher.anyFacet("organization", "text"),
                        FacetEntryMatcher.anyFacet("funding", "text"),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
                .andExpect(
                        jsonPath("$._links.self.href", containsString("configuration=RELATION.Person.researchoutputs")))
                .andExpect(jsonPath("$._links.self.href", containsString("scope=" + uuidSecondPerson)))
        ;
    }
}
