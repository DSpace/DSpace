/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.UUID;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.AppliedFilterMatcher;
import org.dspace.app.rest.matcher.FacetEntryMatcher;
import org.dspace.app.rest.matcher.FacetValueMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.SearchFilterMatcher;
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
import org.dspace.builder.PoolTaskBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public class DiscoveryRestControllerIT extends AbstractControllerIntegrationTest {
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    MetadataAuthorityService metadataAuthorityService;

    @Autowired
    ChoiceAuthorityService choiceAuthorityService;

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
    public void discoverFacetsTestWithoutParameters() throws Exception {

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
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)))
        );
    }

    @Test
    public void discoverFacetsAuthorTestWithSizeParameter() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
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

        //2. Three public items that are readable by Anonymous with different subjects and authors
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
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

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
                //Because we've constructed such a structure so that we have more than 2 (size) authors, there
                // needs to be a next link
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/author?page")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //Because there are more authors than is represented (because of the size param), hasMore has to
                // be true
                //The page object needs to be present and just like specified in the matcher
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 2))))
                //These authors need to be in the response because it's sorted on how many times the author comes
                // up in different items
                //These authors are the most used ones. Only two show up because of the size.
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria")
                )))
        ;
    }

    @Test
    public void discoverFacetsAuthorWithAuthorityWithSizeParameter() throws Exception {
        configurationService.setProperty("choices.plugin.dc.contributor.author", "SolrAuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();

        //Turn off the authorization system, otherwise we can't make the objects
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

        //2. Three public items that are readable by Anonymous with different subjects and authors
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald", "test_authority", Choices.CF_ACCEPTED)
                                      .withAuthor("Doe, John", "test_authority_2", Choices.CF_ACCEPTED)
                                      .withSubject("History of religion")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria", "test_authority_3", Choices.CF_ACCEPTED)
                                      .withAuthor("Doe, Jane", "test_authority_4", Choices.CF_ACCEPTED)
                                      .withSubject("Church studies")
                                      .withSubject("History of religion")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria", "test_authority_3", Choices.CF_ACCEPTED)
                                      .withAuthor("Doe, Jane", "test_authority_4", Choices.CF_ACCEPTED)
                                      .withAuthor("test, test", "test_authority_5", Choices.CF_ACCEPTED)
                                      .withAuthor("test2, test2", "test_authority_6", Choices.CF_ACCEPTED)
                                      .withAuthor("Maybe, Maybe", "test_authority_7", Choices.CF_ACCEPTED)
                                      .withSubject("Missionary studies")
                                      .withSubject("Church studies")
                                      .withSubject("History of religion")
                                      .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 2
        getClient().perform(get("/api/discover/facets/author")
                                .param("size", "2"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type needs to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The name of the facet needs to be seubject, because that's what we called
                   .andExpect(jsonPath("$.name", is("author")))
                   //Because we've constructed such a structure so that we have more than 2 (size) subjects, there
                   // needs to be a next link
                   .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/author?page")))
                   //There always needs to be a self link
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                   //Because there are more subjects than is represented (because of the size param), hasMore has to
                   // be true
                   //The page object needs to be present and just like specified in the matcher
                   .andExpect(jsonPath("$.page",
                                       is(PageMatcher.pageEntry(0, 2))))
                   //These subjecs need to be in the response because it's sorted on how many times the author comes
                   // up in different items
                   //These subjects are the most used ones. Only two show up because of the size.
                   .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                       FacetValueMatcher.entryAuthorWithAuthority("Doe, Jane", "test_authority_4", 2),
                       FacetValueMatcher.entryAuthorWithAuthority("Smith, Maria", "test_authority_3", 2)
                   )));

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();

        metadataAuthorityService.clearCache();
        choiceAuthorityService.clearCache();

    }

    @Test
    public void discoverFacetsAuthorTestWithPrefix() throws Exception {
        //Turn off the authorization system, otherwise we can't make the objects
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

        //2. Three public items that are readable by Anonymous with different subjects and authors
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find the objects in the system and enters a size of 2
        getClient().perform(get("/api/discover/facets/author?prefix=smith")
                .param("size", "10"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type needs to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The name of the facet needs to be author, because that's what we called
                .andExpect(jsonPath("$.name", is("author")))
                //The facetType has to be 'text' because that's how the author facet is configured by default
                .andExpect(jsonPath("$.facetType", is("text")))
                //We only request value starting with "smith", so we expect to only receive one page
                .andExpect(jsonPath("$._links.next").doesNotExist())
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?prefix=smith")))
                //Because there are more authors than is represented (because of the size param), hasMore has to
                // be true
                //The page object needs to be present and just like specified in the matcher
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 10))))
                //These authors need to be in the response because it's sorted on how many times the author comes
                // up in different items
                //These authors are order according to count. Only two show up because of the prefix.
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Smith, Maria"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )))
        ;
    }

    @Test
    public void discoverFacetsAuthorTestForHasMoreFalse() throws Exception {
        //Turn of the authorization system so that we can create the structure specified below
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

        context.restoreAuthSystemState();

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
                //The page object needs to present and exactly like how it is specified here. 20 is entered as the
                // size because that's the default in the configuration if no size parameter has been given
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //The authors need to be embedded in the values, all 4 of them have to be present as the size
                // allows it
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria"),
                        FacetValueMatcher.entryAuthor("Doe, John"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )))
        ;
    }

    @Test
    public void discoverFacetsAuthorTestForPagination() throws Exception {

        //We turn off the authorization system in order to construct the structure defined below
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
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withAuthor("Smith, Maria")
                .withAuthor("Doe, Jane")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Doe, John")
                .withAuthor("Smith, Donald")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

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
                //There needs to be a next link because there are more authors than the current size is allowed to
                // show. There are more pages after this one
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/author?page")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
                //The page object has to be like this because that's what we've asked in the parameters
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(1, 2))))
                //These authors have to be present because of the current configuration
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, John"),
                        FacetValueMatcher.entryAuthor("Smith, Donald")
                )))
        ;
    }


    @Test
    public void discoverFacetsTestWithSimpleQueryAndSearchFilter() throws Exception {
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
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

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
                //The self link needs to contain the query that was specified in the parameters, this is how it
                // looks like
                .andExpect(jsonPath("$._links.self.href", containsString("f.title=test,contains")))
                //The applied filters have to be specified like this, applied filters are the parameters given
                // below starting with f.
                .andExpect(jsonPath("$.appliedFilters", contains(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //This is how the page object must look like because it's the default
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //These authors need to be present in the result because these have made items that contain 'test'
                // in the title
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
    public void discoverFacetsDateTest() throws Exception {
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
                .withTitle("Public item 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2000-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

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
                //This is how the page object must look like because it's the default with size 20
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 20))))
                //The date values need to be as specified below
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        //We'll always get atleast two intervals with the items specified above, so we ask to match
                        // twice atleast
                        FacetValueMatcher.entryDateIssued(),
                        FacetValueMatcher.entryDateIssued()
                )))
        ;
    }


    @Test
    public void discoverFacetsTestWithScope() throws Exception {
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

        context.restoreAuthSystemState();

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
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?scope=testScope")))
                //These are all the authors for the items that were created and thus they have to be present in
                // the embedded values section
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
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?scope=testScope")))
                .andExpect(jsonPath("$._links.next.href",
                     containsString("api/discover/facets/author?scope=testScope&page=1&size=2")))
                //These are the values that need to be present as it's ordered by count and these authors are the
                // most common ones in the items that we've created
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryAuthor("Doe, Jane"),
                        FacetValueMatcher.entryAuthor("Smith, Maria")
                )))
        ;
    }

    @Test
    public void discoverFacetsDateTestForHasMore() throws Exception {
        //We turn off the authorization system to create the structure defined below
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

        //2. 7 public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald").withAuthor("Testing, Works")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("Testing, Works")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1940-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem4 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1950-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem5 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1960-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem6 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1970-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem7 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("1980-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

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
                //the facetType needs to be 'date' as that's the default facetType for this facet in the
                // configuration
                .andExpect(jsonPath("$.facetType", is("date")))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
                //Seeing as we've entered a size of two and there are more dates than just two, we'll need a next
                // link to go to the next page to see the rest of the dates
                .andExpect(jsonPath("$._links.next.href", containsString("api/discover/facets/dateIssued?page")))
                //The page object needs to look like this because we've entered a size of 2 and we didn't specify
                // a starting page so it defaults to 0
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 2))))
                //There needs to be two date results in the embedded values section because that's what we've
                // specified
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryDateIssued(),
                        FacetValueMatcher.entryDateIssued()
                )))
        ;
    }


    @Test
    public void discoverFacetsDateTestWithSearchFilter() throws Exception {

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
                //The facetType needs to be 'date' as that's the default facetType for this facet in the
                // configuration
                .andExpect(jsonPath("$.facetType", is("date")))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
                //There needs to be an appliedFilters section that looks like this because we've specified a query
                // in the parameters
                .andExpect(jsonPath("$.appliedFilters", containsInAnyOrder(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //The page object needs to look like this because we entered a size of 2 and we didn't specify a
                // starting page so it defaults to 0
                .andExpect(jsonPath("$.page",
                        is(PageMatcher.pageEntry(0, 2))))
                //There needs to be only two date intervals with a count of 1 because of the query we specified
                .andExpect(jsonPath("$._embedded.values", containsInAnyOrder(
                        FacetValueMatcher.entryDateIssuedWithCountOne(),
                        FacetValueMatcher.entryDateIssuedWithCountOne()
                )))
        ;
    }


    @Test
    public void discoverSearchTest() throws Exception {

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
                   //There needs to be a section where these filters as specified as they're the default filters
                   // given in the configuration
                   .andExpect(jsonPath("$.filters", containsInAnyOrder(
                       SearchFilterMatcher.titleFilter(),
                       SearchFilterMatcher.authorFilter(),
                       SearchFilterMatcher.subjectFilter(),
                       SearchFilterMatcher.dateIssuedFilter(),
                       SearchFilterMatcher.hasContentInOriginalBundleFilter(),
                       SearchFilterMatcher.hasFileNameInOriginalBundleFilter(),
                       SearchFilterMatcher.hasFileDescriptionInOriginalBundleFilter(),
                       SearchFilterMatcher.entityTypeFilter(),
                       SearchFilterMatcher.isAuthorOfPublicationRelation(),
                       SearchFilterMatcher.isProjectOfPublicationRelation(),
                       SearchFilterMatcher.isOrgUnitOfPublicationRelation(),
                       SearchFilterMatcher.isPublicationOfJournalIssueRelation(),
                       SearchFilterMatcher.isJournalOfPublicationRelation()
                   )))
                   //These sortOptions need to be present as it's the default in the configuration
                   .andExpect(jsonPath("$.sortOptions", contains(
                       SortOptionMatcher.sortOptionMatcher(
                                         "score", DiscoverySortFieldConfiguration.SORT_ORDER.desc.name()),
                       SortOptionMatcher.sortOptionMatcher(
                                         "dc.title", DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher(
                                         "dc.date.issued", DiscoverySortFieldConfiguration.SORT_ORDER.desc.name()),
                       SortOptionMatcher.sortOptionMatcher(
                                         "dc.date.accessioned", DiscoverySortFieldConfiguration.SORT_ORDER.desc.name())
                   )));
    }

    @Test
    public void checkSortOrderInPersonOrOrgunitConfigurationTest() throws Exception {
        getClient().perform(get("/api/discover/search")
                   .param("configuration", "personOrOrgunit"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$._links.objects.href", containsString("api/discover/search/objects")))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/search")))
                   .andExpect(jsonPath("$.sortOptions", contains(
                       SortOptionMatcher.sortOptionMatcher("dspace.entity.type",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.desc.name()),
                       SortOptionMatcher.sortOptionMatcher("organization.legalName",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher("organisation.address.addressCountry",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher("organisation.address.addressLocality",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher("organisation.foundingDate",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.desc.name()),
                       SortOptionMatcher.sortOptionMatcher("dc.date.accessioned",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.desc.name()),
                       SortOptionMatcher.sortOptionMatcher("person.familyName",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher("person.givenName",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.asc.name()),
                       SortOptionMatcher.sortOptionMatcher("person.birthDate",
                                         DiscoverySortFieldConfiguration.SORT_ORDER.desc.name())
                    )));
    }

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

        getClient().perform(get("/api/discover/search/objects")
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
        getClient().perform(get("/api/discover/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
                get("/api/discover/search/objects")
                        .param("sort", "score,DESC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.facets", hasItem(allOf(
                        hasJsonPath("$.name", is("author")),
                        hasJsonPath("$._embedded.values", hasItem(
                                hasJsonPath("$._links.search.href", containsString("DSpace%20%26%20friends"))
                        ))
                ))));
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
    public void discoverSearchObjectsTestHasMoreAuthorFacet() throws Exception {
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
                .withAuthor("Smith, Donald").withAuthor("Testing, Works").withAuthor("a1, a1")
                .withAuthor("b, b").withAuthor("c, c").withAuthor("d, d").withAuthor("e, e")
                .withAuthor("f, f").withAuthor("g, g")
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
        getClient().perform(get("/api/discover/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because we've only made 7 elements, the default size is 20
                // and they all fit onto one page (20 > 7) so totalPages has to be 1. Number is 0 because
                //page 0 is the default page we view if not specified otherwise
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
                )))
                //All elements have to be present in the embedded.objects section, these are the ones we made in
                // the structure defined above
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                        SearchResultMatcher.match("core", "community", "communities"),
                        SearchResultMatcher.match("core", "community", "communities"),
                        //Match without any parameters because collections don't have anything special to check in the
                        // json
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match("core", "item", "items"),
                        SearchResultMatcher.match("core", "item", "items"),
                        SearchResultMatcher.match("core", "item", "items")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                //We do however exceed the limit for the authors, so this property has to be true for the author
                // facet
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(true),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }


    @Test
    public void discoverSearchObjectsTestHasMoreSubjectFacet() throws Exception {
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
                .withSubject("ExtraEntry").withSubject("a")
                .withSubject("b").withSubject("c")
                .withSubject("d").withSubject("e")
                .withSubject("f").withSubject("g")
                .withSubject("h").withSubject("i")
                .withSubject("j").withSubject("k")
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
        //An anonymous user browses this endpoint to find the the objects in the system
        getClient().perform(get("/api/discover/search/objects"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because we've only made 7 items, they all fit onto 1 page
                // because the default size is 20 and the default starting page is 0.
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 7)
                )))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                //We do however exceed the limit for the subject, so this property has to be true for the subject
                // facet
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(true),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a query that says that the title has to contain 'test'
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "test,contains"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //We need to display the appliedFilters object that contains the query that we've ran
                .andExpect(jsonPath("$.appliedFilters", contains(
                        AppliedFilterMatcher.appliedFilterEntry("title", "contains", "test", "test")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        getClient().perform(get("/api/discover/search/objects")
                .param("query", "test"))
                .andExpect(status().isOk());

        getClient().perform(get("/api/discover/search/objects")
                .param("query", "test:"))
                .andExpect(status().isUnprocessableEntity());

    }


    @Test
    public void discoverSearchObjectsTestWithInvalidSolrQuery() throws Exception {

        getClient().perform(get("/api/discover/search/objects")
                .param("query", "test"))
                .andExpect(status().isOk());

        getClient().perform(get("/api/discover/search/objects")
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        // An anonymous user browses this endpoint to find the the objects in the system

        // With dsoType 'item'
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "Item"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        // With dsoTypes 'community' and 'collection'
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "Community")
                .param("dsoType", "Collection"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        // With dsoTypes 'collection' and 'item'
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "Collection")
                .param("dsoType", "Item"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        // With dsoTypes 'community', 'collection' and 'item'
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "Community")
                .param("dsoType", "Collection")
                .param("dsoType", "Item"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a dsoType 'item'
        //And a sort on the dc.title ascending
        getClient().perform(get("/api/discover/search/objects")
                .param("dsoType", "Item")
                .param("sort", "dc.title,ASC"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //We want to get the sort that's been used as well in the response
                .andExpect(jsonPath("$.sort", is(
                        SortOptionMatcher.sortByAndOrder("dc.title", "ASC")
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;
    }


    // This test has been disable due to its innate dependency on knowing the facetLimit
    // This is currently untrue and resulted in hardcoding of expectations.
    @Test
    @Ignore
    public void discoverFacetsDateTestWithLabels() throws Exception {
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

        //2. 9 public items that are readable by Anonymous with different subjects
        Item publicItem6 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("2017-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem7 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("2010-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem8 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1990-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem9 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1970-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem10 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1950-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem11 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1930-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem12 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1910-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem13 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1890-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();
        Item publicItem14 = ItemBuilder.createItem(context, col2)
                .withTitle("Public")
                .withIssueDate("1866-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withAuthor("test,test")
                .withAuthor("test2, test2").withAuthor("Maybe, Maybe")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses this endpoint to find dateIssued facet values
        getClient().perform(get("/api/discover/facets/dateIssued"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object has to look like this because the default size is 20 and the default starting
                // page is 0
                .andExpect(jsonPath("$.page", is(
                        PageMatcher.pageEntry(0, 20)
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
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 4, 7)
                )))
                //These are the  two elements that'll be shown (because page = 1, so the third and fourth element
                // in the list) and they'll be the only ones because the size is 2
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match()
                )))
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(true),
                        FacetEntryMatcher.subjectFacet(true),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a query stating 'ThisIsSomeDummyText'
        getClient().perform(get("/api/discover/search/objects")
                .param("query", "ThisIsSomeDummyText"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //This is the only item that should be returned with the query given
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test")
                )))

                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
                .withEmbargoPeriod("12 months")
                .build();

        //Turn on the authorization again
        context.restoreAuthSystemState();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //
        getClient().perform(get("/api/discover/search/objects"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/search/objects")
                .param("query", "ThisIsSomeDummyText"))

                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //Make sure that the item with the private bitstream doesn't show up
                .andExpect(jsonPath("$._embedded.object", Matchers.not(Matchers.contains(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test")
                ))))

                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the scope given
        getClient().perform(get("/api/discover/search/objects")
                .param("scope", String.valueOf(scope)))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The scope has to be equal to the one given in the parameters
                .andExpect(jsonPath("$.scope", is(String.valueOf(scope))))
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items belonging to the scope specified
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/search/objects")
                .param("scope", String.valueOf(scope)))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //Make sure that the scope is set to the scope given in the param
                .andExpect(jsonPath("$.scope", is(String.valueOf(scope))))
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }

    /**
     * This test verifies that
     * {@link org.dspace.discovery.indexobject.InprogressSubmissionIndexFactoryImpl#storeInprogressItemFields}
     * indexes the owning collection of workspace items
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
            get("/api/discover/search/objects")
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
     * indexes the owning collection of workflow items
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
            get("/api/discover/search/objects")
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a query stating 'public'
        getClient().perform(get("/api/discover/search/objects")
                .param("query", query))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a query stating 'Public'
        getClient().perform(get("/api/discover/search/objects")
                .param("query", query))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "test*,query"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items that match the searchFilter
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Test 2")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property
                // because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "test,contains"))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                                                                          )))
                   //The search results have to contain the items that match the searchFilter
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                       SearchResultMatcher.matchOnItemName("item", "items", "Test"),
                       SearchResultMatcher.matchOnItemName("item", "items", "Test 2")
                                                                                                                )))
                   //These facets have to show up in the embedded.facets section as well with the given hasMore property
                   // because we don't exceed their default limit for a hasMore true (the default is 10)
                   .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                       FacetEntryMatcher.authorFacet(false),
                       FacetEntryMatcher.entityTypeFacet(false),
                       FacetEntryMatcher.subjectFacet(false),
                       FacetEntryMatcher.dateIssuedFacet(false),
                       FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                                                                                        )))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "-test*,query"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items that match the searchFilter
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property
                // because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "test,notcontains"))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                                                                          )))
                   //The search results have to contain the items that match the searchFilter
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                       SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                                                                                                     )))
                   //These facets have to show up in the embedded.facets section as well with the given hasMore property
                   // because we don't exceed their default limit for a hasMore true (the default is 10)
                   .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                       FacetEntryMatcher.authorFacet(false),
                       FacetEntryMatcher.entityTypeFacet(false),
                       FacetEntryMatcher.subjectFacet(false),
                       FacetEntryMatcher.dateIssuedFacet(false),
                       FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                                                                                        )))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(1, 2, 4, 7)
                )))
                //These are the  two elements that'll be shown (because page = 1, so the third and fourth element
                // in the list) and they'll be the only ones because the size is 2
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                        SearchResultMatcher.match(),
                        SearchResultMatcher.match()
                )))
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetWithMinMax(true, "Doe, Jane", "Testing, Works"),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(true),
                        FacetEntryMatcher.dateIssuedFacetWithMinMax(false, "1990-02-13", "2010-10-17"),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }

    @Test
    public void discoverSearchFacetsTestForMinMaxValues() throws Exception {
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
        //An anonymous user browses this endpoint to find the the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/search/facets"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacetWithMinMax(true, "Doe, Jane", "Testing, Works"),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(true),
                        FacetEntryMatcher.dateIssuedFacetWithMinMax(false, "1990-02-13", "2010-10-17"),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/facets")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "Test,query"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items that match the searchFilter
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property
                // because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "Test,equals"))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                                                                          )))
                   //The search results have to contain the items that match the searchFilter
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.containsInAnyOrder(
                       SearchResultMatcher.matchOnItemName("item", "items", "Test")
                                                                                                                )))
                   //These facets have to show up in the embedded.facets section as well with the given hasMore property
                   // because we don't exceed their default limit for a hasMore true (the default is 10)
                   .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                       FacetEntryMatcher.authorFacet(false),
                       FacetEntryMatcher.entityTypeFacet(false),
                       FacetEntryMatcher.subjectFacet(false),
                       FacetEntryMatcher.dateIssuedFacet(false),
                       FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                                                                                        )))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "-Test,query"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items that match the searchFilter
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                        SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property
                // because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "Test,notequals"))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                                                                          )))
                   //The search results have to contain the items that match the searchFilter
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItems(
                       SearchResultMatcher.matchOnItemName("item", "items", "Test 2"),
                       SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                                                                                                      )))
                   //These facets have to show up in the embedded.facets section as well with the given hasMore property
                   // because we don't exceed their default limit for a hasMore true (the default is 10)
                   .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                       FacetEntryMatcher.authorFacet(false),
                       FacetEntryMatcher.entityTypeFacet(false),
                       FacetEntryMatcher.subjectFacet(false),
                       FacetEntryMatcher.dateIssuedFacet(false),
                       FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                                                                                        )))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
                .param("f.title", "-id:test,query"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //The page object needs to look like this
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntry(0, 20)
                )))
                //The search results have to contain the items that match the searchFilter
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                )))
                //These facets have to show up in the embedded.facets section as well with the given hasMore property
                // because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.authorFacet(false),
                        FacetEntryMatcher.entityTypeFacet(false),
                        FacetEntryMatcher.subjectFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                )))
                //There always needs to be a self link available
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        UUID scope = col2.getID();
        //** WHEN **
        //An anonymous user browses this endpoint to find the the objects in the system
        //With the given search filter
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "test,notauthority"))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                                                                          )))
                   //The search results have to contain the items that match the searchFilter
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.hasItem(
                       SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                                                                                                     )))
                   //These facets have to show up in the embedded.facets section as well with the given hasMore property
                   // because we don't exceed their default limit for a hasMore true (the default is 10)
                   .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                       FacetEntryMatcher.authorFacet(false),
                       FacetEntryMatcher.entityTypeFacet(false),
                       FacetEntryMatcher.subjectFacet(false),
                       FacetEntryMatcher.dateIssuedFacet(false),
                       FacetEntryMatcher.hasContentInOriginalBundleFacet(false)
                                                                                        )))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

    }

    @Test
    public void discoverSearchObjectsWithMissingQueryOperator() throws Exception {
        //** WHEN **
        // An anonymous user browses this endpoint to find the the objects in the system
        // With the given search filter where there is the filter operator missing in the value (must be of form
        // <:filter-value>,<:filter-operator>)
        getClient().perform(get("/api/discover/search/objects")
            .param("f.title", "test"))
                   //** THEN **
                   //Will result in 422 status because of missing filter operator
                   .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void discoverSearchObjectsWithNotValidQueryOperator() throws Exception {
        //** WHEN **
        // An anonymous user browses this endpoint to find the the objects in the system
        // With the given search filter where there is a non-valid filter operator given (must be of form
        // <:filter-value>,<:filter-operator> where the filter operator is one of: “contains”, “notcontains”, "equals"
        // “notequals”, “authority”, “notauthority”, "query”); see enum RestSearchOperator
        getClient().perform(get("/api/discover/search/objects")
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
        getClient().perform(get("/api/discover/search/objects")
                                .param("query", "dc.date.issued:\"2010-02-13\""))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                   )))
                   //This is the only item that should be returned with the query given
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                       SearchResultMatcher.matchOnItemName("item", "items", "Public item 2")
                   )))

                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;


        //** WHEN **
        getClient().perform(get("/api/discover/search/objects")
                                .param("query", "dc.date.issued:\"2013-02-13\""))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                   )))

                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        getClient().perform(get("/api/discover/search/objects")
                                .param("query", "((dc.date.issued:2010 OR dc.date.issued:1990-02-13)" +
                                                                " AND (dc.title:Test OR dc.title:TestItem2))"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
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
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        getClient().perform(get("/api/discover/search/objects")
                                .param("query", "\"Faithful Infidel: Exploring Conformity (2nd edition)\""))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
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
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        getClient().perform(get("/api/discover/search/objects")
                                .param("query", "OR"))

                   .andExpect(status().isUnprocessableEntity())
        ;

    }

    @Test
    /**
     * This test is intent to verify that inprogress submission (workspaceitem, workflowitem, pool task and claimed
     * tasks) don't interfers with the standard search
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

        //3. three inprogress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
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

        // 5. other inprogress submissions made by the administrator
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
        // system should not retrieve the inprogress submissions and related objects
        String[] tokens = new String[] {
            null,
            getAuthToken(eperson.getEmail(), password),
            getAuthToken(admin.getEmail(), password),
        };

        for (String token : tokens) {
            getClient(token).perform(get("/api/discover/search/objects"))
                    //** THEN **
                    //The status has to be 200 OK
                    .andExpect(status().isOk())
                    //The type has to be 'discover'
                    .andExpect(jsonPath("$.type", is("discover")))
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
                    //These facets have to show up in the embedded.facets section as well with the given hasMore
                    // property because we don't exceed their default limit for a hasMore true (the default is 10)
                    .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                            FacetEntryMatcher.authorFacet(false),
                            FacetEntryMatcher.subjectFacet(false),
                            FacetEntryMatcher.dateIssuedFacet(false),
                            FacetEntryMatcher.hasContentInOriginalBundleFacet(false),
                            FacetEntryMatcher.entityTypeFacet(false)
                    )))
                    //There always needs to be a self link
                    .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        //3. three inprogress submission from our submitter user (2 ws, 1 wf that will produce also a pooltask)
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

        // 5. other inprogress submissions made by the administrator
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
        // each submitter, including the administrator should see only her submission
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(submitterToken).perform(get("/api/discover/search/objects").param("configuration", "workspace"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        getClient(adminToken).perform(get("/api/discover/search/objects").param("configuration", "workspace"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        //3. three inprogress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
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

        // 5. other inprogress submissions made by the administrator
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

        // 6. a pool taks in the second step of the workflow
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
        getClient(epersonToken).perform(get("/api/discover/search/objects").param("configuration", "workflow"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a page object that shows the total pages and total elements as well as the
                // size and the current page (number)
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        // reviewer1 should see two pool items, one from the submitter and one from the administrator
        // the other task in step1 is claimed by the administrator so it should be not visible to the reviewer1
        getClient(reviewer1Token).perform(get("/api/discover/search/objects").param("configuration", "workflow"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.submitterFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        // admin should see two pool items and a claimed task,
        // one pool item from the submitter and one from herself
        // because the admin is in the reviewer group for step 1, not because she is an admin
        getClient(adminToken).perform(get("/api/discover/search/objects").param("configuration", "workflow"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.submitterFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        // reviewer2 should only see one pool item
        getClient(reviewer2Token).perform(get("/api/discover/search/objects").param("configuration", "workflow"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.submitterFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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

        //3. three inprogress submission from a normal user (2 ws, 1 wf that will produce also a pooltask)
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

        // 5. other inprogress submissions made by the administrator
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

        // 6. a pool taks in the second step of the workflow
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
        getClient(epersonToken).perform(get("/api/discover/search/objects").param("configuration", "workflowAdmin"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a page object that shows the total pages and total elements as well as the
                // size and the current page (number)
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        // reviewer1 should not see pool items, as he is not an administrator
        getClient(reviewer1Token).perform(get("/api/discover/search/objects").param("configuration", "workflowAdmin"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a page object that shows the total pages and total elements as well as the
                // size and the current page (number)
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;


        // admin should see three pool items and a claimed task
        // one pool item from the submitter and two from herself
        getClient(adminToken).perform(get("/api/discover/search/objects").param("configuration", "workflowAdmin"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
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
                //These facets have to show up in the embedded.facets section as well with the given hasMore
                // property because we don't exceed their default limit for a hasMore true (the default is 10)
                .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                        FacetEntryMatcher.resourceTypeFacet(false),
                        FacetEntryMatcher.typeFacet(false),
                        FacetEntryMatcher.dateIssuedFacet(false),
                        FacetEntryMatcher.submitterFacet(false)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
        ;

        // reviewer2 should not see pool items, as he is not an administrator
        getClient(reviewer2Token).perform(get("/api/discover/search/objects").param("configuration", "workflowAdmin"))
                //** THEN **
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //The type has to be 'discover'
                .andExpect(jsonPath("$.type", is("discover")))
                //There needs to be a page object that shows the total pages and total elements as well as the
                // size and the current page (number)
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 0, 0)
                )))
                //There always needs to be a self link
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))
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
        getClient().perform(get("/api/discover/search/objects")
            .param("configuration", "undiscoverable")
            .param("query", query))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
                   //The page object needs to look like this
                   .andExpect(jsonPath("$._embedded.searchResult.page", is(
                       PageMatcher.pageEntry(0, 20)
                   )))
                   //The search results should be an empty list.
                   .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.empty()))
                   //There always needs to be a self link available
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))

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
        getClient(adminToken).perform(get("/api/discover/search/objects")
            .param("configuration", "undiscoverable")
            .param("query", query))
                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //The type has to be 'discover'
                   .andExpect(jsonPath("$.type", is("discover")))
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
                   .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")))

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

        getClient().perform(get("/api/discover/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

                //** THEN **

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item")
                )))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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

        getClient(authToken).perform(get("/api/discover/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

                //** THEN **

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                        SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item")
                )))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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

        getClient(adminToken).perform(get("/api/discover/search/objects")
                .param("configuration", "administrativeView")
                .param("query", "Test"))

                //** THEN **

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
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
                .andExpect(jsonPath("$._embedded.facets", Matchers.hasItems(
                        allOf(
                                hasJsonPath("$.name", is("discoverable")),
                                hasJsonPath("$._embedded.values", Matchers.hasItems(
                                        allOf(
                                                hasJsonPath("$.label", is("true")),
                                                hasJsonPath("$.count", is(2))
                                        ),
                                        allOf(
                                                hasJsonPath("$.label", is("false")),
                                                hasJsonPath("$.count", is(1))
                                        )
                                ))
                        ),
                        allOf(
                                hasJsonPath("$.name", is("withdrawn")),
                                hasJsonPath("$._embedded.values", Matchers.hasItems(
                                        allOf(
                                                hasJsonPath("$.label", is("true")),
                                                hasJsonPath("$.count", is(1))
                                        ),
                                        allOf(
                                                hasJsonPath("$.label", is("false")),
                                                hasJsonPath("$.count", is(2))
                                        )
                                ))
                        )
                )))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.withdrawn", "true,contains")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.contains(
                                SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.withdrawn", "false,contains")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.containsInAnyOrder(
                                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                                SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.discoverable", "true,contains")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.containsInAnyOrder(
                                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                                SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.discoverable", "false,contains")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.contains(
                                SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.withdrawn", "true,equals")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.contains(
                                SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.withdrawn", "false,equals")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.containsInAnyOrder(
                                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                                SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.discoverable", "true,equals")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.containsInAnyOrder(
                                SearchResultMatcher.matchOnItemName("item", "items", "Public Test Item"),
                                SearchResultMatcher.matchOnItemName("item", "items", "Withdrawn Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));

        getClient(adminToken)
                .perform(get("/api/discover/search/objects")
                        .param("configuration", "administrativeView")
                        .param("query", "Test")
                        .param("f.discoverable", "false,equals")
                )

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("discover")))
                .andExpect(jsonPath("$._embedded.searchResult.page", is(
                        PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)
                )))
                .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",
                        Matchers.contains(
                                SearchResultMatcher.matchOnItemName("item", "items", "Private Test Item")
                        )
                ))
                .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/search/objects")));
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

        getClient(adminToken).perform(get("/api/discover/search/objects")
                             .param("configuration", "workflow")
                             .param("sort", "dc.date.issued,DESC")
                             .param("query", "Mathematical Theory"))
                         .andExpect(status().isOk())
                         .andExpect(jsonPath("$.query", is("Mathematical Theory")))
                         .andExpect(jsonPath("$.configuration", is("workflow")))
                         .andExpect(jsonPath("$._embedded.searchResult._embedded.objects", Matchers.contains(
                                             SearchResultMatcher.match("workflow", "pooltask", "pooltasks")
                          )))
                         .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",Matchers.contains(
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Mathematical Theory"))))
                          )))
                         .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(1)));

        getClient(adminToken).perform(get("/api/discover/search/objects")
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
                         .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",Matchers.containsInAnyOrder(
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Metaphysics")))),
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Test Metaphysics"))))
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

        getClient(adminToken).perform(get("/api/discover/search/objects")
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
                         .andExpect(jsonPath("$._embedded.searchResult._embedded.objects",Matchers.containsInAnyOrder(
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Mathematical Theory")))),
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Metaphysics")))),
                              allOf(hasJsonPath("$._embedded.indexableObject._embedded.workflowitem._embedded.item",
                                 is(SearchResultMatcher.matchEmbeddedObjectOnItemName("item", "Test Metaphysics"))))
                          )))
                         .andExpect(jsonPath("$._embedded.searchResult.page.totalElements", is(3)));
    }


    @Test
    public void discoverSearchFacetValuesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public Test Item")
                   .withIssueDate("2010-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry").build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Withdrawn Test Item")
                   .withIssueDate("1990-02-13")
                   .withAuthor("Smith, Maria")
                   .withAuthor("Doe, Jane")
                   .withSubject("ExtraEntry")
                   .withdrawn().build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Private Test Item")
                   .withIssueDate("2010-02-13")
                   .withAuthor("Smith, Maria")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .makeUnDiscoverable().build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/discover/facets/discoverable")
                 .param("configuration", "administrativeView")
                 .param("sort", "score,DESC")
                 .param("page", "0")
                 .param("size", "10"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._links.self.href",containsString(
                  "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC")))
                 .andExpect(jsonPath("$._embedded.values", Matchers.containsInAnyOrder(
                            SearchResultMatcher.matchEmbeddedFacetValues("true", 2, "discover",
                            "/api/discover/search/objects?configuration=administrativeView&f.discoverable=true,equals"),
                            SearchResultMatcher.matchEmbeddedFacetValues("false", 1, "discover",
                            "/api/discover/search/objects?configuration=administrativeView&f.discoverable=false,equals")
                            )));

    }

    @Test
    public void discoverSearchFacetValuesPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1").build();

        Collection col2 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 2").build();

        ItemBuilder.createItem(context, col1)
                   .withTitle("Public Test Item")
                   .withIssueDate("2010-10-17")
                   .withAuthor("Smith, Donald")
                   .withSubject("ExtraEntry").build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Withdrawn Test Item")
                   .withIssueDate("1990-02-13")
                   .withAuthor("Smith, Maria")
                   .withAuthor("Doe, Jane")
                   .withSubject("ExtraEntry")
                   .withdrawn().build();

        ItemBuilder.createItem(context, col2)
                   .withTitle("Private Test Item")
                   .withIssueDate("2010-02-13")
                   .withAuthor("Smith, Maria")
                   .withAuthor("Doe, Jane")
                   .withSubject("AnotherTest")
                   .withSubject("ExtraEntry")
                   .makeUnDiscoverable().build();

        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);

        getClient(adminToken).perform(get("/api/discover/facets/discoverable")
                 .param("configuration", "administrativeView")
                 .param("sort", "score,DESC")
                 .param("page", "0")
                 .param("size", "1"))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$._links.self.href",containsString(
                  "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC")))
                 .andExpect(jsonPath("$._links.next.href",containsString(
                  "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC&page=1&size=1")))
                 .andExpect(jsonPath("$._embedded.values", Matchers.contains(
                            SearchResultMatcher.matchEmbeddedFacetValues("true", 2, "discover",
                            "/api/discover/search/objects?configuration=administrativeView&f.discoverable=true,equals")
                            )));

        getClient(adminToken).perform(get("/api/discover/facets/discoverable")
                .param("configuration", "administrativeView")
                .param("sort", "score,DESC")
                .param("page", "1")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.first.href",containsString(
                 "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC&page=0&size=1")))
                .andExpect(jsonPath("$._links.prev.href",containsString(
                 "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC&page=0&size=1")))
                .andExpect(jsonPath("$._links.self.href",containsString(
                 "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC&page=1&size=1")))
                .andExpect(jsonPath("$._embedded.values", Matchers.contains(
                           SearchResultMatcher.matchEmbeddedFacetValues("false", 1, "discover",
                           "/api/discover/search/objects?configuration=administrativeView&f.discoverable=false,equals")
                           )));
    }

    @Test
    public void discoverFacetsTestWithQueryTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2019-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2020-02-13")
                .withAuthor("Doe, Jane")
                .withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2020-02-13")
                .withAuthor("Anton, Senek")
                .withSubject("AnotherTest").withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/author")
                   .param("query", "Donald"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$.name", is("author")))
                   .andExpect(jsonPath("$.facetType", is("text")))
                   .andExpect(jsonPath("$.scope", is(emptyOrNullString())))
                   .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?query=Donald")))
                   .andExpect(jsonPath("$._embedded.values[0].label", is("Smith, Donald")))
                   .andExpect(jsonPath("$._embedded.values[0].count", is(1)))
                   .andExpect(jsonPath("$._embedded.values[0]._links.search.href",
                        containsString("api/discover/search/objects?query=Donald&f.author=" +
                                urlPathSegmentEscaper().escape("Smith, Donald,equals")
                        )))
                   .andExpect(jsonPath("$._embedded.values").value(Matchers.hasSize(1)));

    }

    @Test
    public void discoverFacetsTestWithDsoTypeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community").build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 2").build();

        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Public item 1")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2020-02-13")
                .withAuthor("Doe, Jane")
                .withSubject("ExtraEntry")
                .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                .withTitle("Public item 2")
                .withIssueDate("2020-02-13")
                .withAuthor("Anton, Senek")
                .withSubject("TestingForMore")
                .withSubject("ExtraEntry")
                .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/dateIssued")
                   .param("dsoType", "Item"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.type", is("discover")))
                   .andExpect(jsonPath("$.name", is("dateIssued")))
                   .andExpect(jsonPath("$.facetType", is("date")))
                   .andExpect(jsonPath("$.scope", is(emptyOrNullString())))
                   .andExpect(jsonPath("$._links.self.href",
                        containsString("api/discover/facets/dateIssued?dsoType=Item")))
                   .andExpect(jsonPath("$._embedded.values[0].label", is("2017 - 2020")))
                   .andExpect(jsonPath("$._embedded.values[0].count", is(3)))
                   .andExpect(jsonPath("$._embedded.values[0]._links.search.href",
                        containsString("api/discover/search/objects?dsoType=Item&f.dateIssued=" +
                                urlPathSegmentEscaper().escape("[2017 TO 2020],equals")
                        )))
                   .andExpect(jsonPath("$._embedded.values").value(Matchers.hasSize(1)));

    }
}
