/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.google.common.net.UrlEscapers.urlPathSegmentEscaper;
import static org.dspace.app.rest.matcher.FacetEntryMatcher.defaultFacetMatchers;
import static org.dspace.app.rest.matcher.FacetValueMatcher.entrySupervisedBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SupervisionOrderBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.supervision.SupervisionOrder;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchFacetRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    ConfigurationService configurationService;

    @Autowired
    MetadataAuthorityService metadataAuthorityService;

    @Autowired
    ChoiceAuthorityService choiceAuthorityService;

    @Test
    public void discoverFacetsTestWithoutParameters() throws Exception {

        //When we call this facets endpoint
        getClient().perform(get("/api/discover/facets"))

            //We expect a 200 OK status
            .andExpect(status().isOk())
            //There needs to be a self link to this endpoint
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets")))
            //We have 4 facets in the default configuration, they need to all be present in the embedded section
            .andExpect(jsonPath("$._embedded.facets", containsInAnyOrder(defaultFacetMatchers)));
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
                .param("size", "2")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name of the facet needs to be author, because that's what we called
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' because that's how the author facet is configured by default
            .andExpect(jsonPath("$.facetType", is("text")))
            //Because we've constructed such a structure so that we have more than 2 (size) authors, there
            // needs to be a next link
            .andExpect(jsonPath("$._embedded.values._links.next.href",
                containsString("api/discover/facets/author")))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
            //Because there are more authors than is represented (because of the size param), hasMore has to
            // be true
            //The page object needs to be present and just like specified in the matcher
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 2))))
            //These authors need to be in the response because it's sorted on how many times the author comes
            // up in different items
            //These authors are the most used ones. Only two show up because of the size.
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("size", "2")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name of the facet needs to be author, because that's what we called
            .andExpect(jsonPath("$.name", is("author")))
            //Because we've constructed such a structure so that we have more than 2 (size) subjects, there
            // needs to be a next link
            .andExpect(jsonPath("$._embedded.values._links.next.href",
                containsString("api/discover/facets/author")))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
            //Because there are more subjects than is represented (because of the size param), hasMore has to
            // be true
            //The page object needs to be present and just like specified in the matcher
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 2))))
            //These subjecs need to be in the response because it's sorted on how many times the author comes
            // up in different items
            //These subjects are the most used ones. Only two show up because of the size.
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("size", "10")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type needs to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name of the facet needs to be author, because that's what we called
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' because that's how the author facet is configured by default
            .andExpect(jsonPath("$.facetType", is("text")))
            //We only request value starting with "smith", so we expect to only receive one page
            .andExpect(jsonPath("$._embedded.values._links.next").doesNotExist())
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?prefix=smith")))
            //Because there are more authors than is represented (because of the size param), hasMore has to
            // be true
            //The page object needs to be present and just like specified in the matcher
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 10))))
            //These authors need to be in the response because it's sorted on how many times the author comes
            // up in different items
            //These authors are order according to count. Only two show up because of the prefix.
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entryAuthor("Smith, Maria"),
                FacetValueMatcher.entryAuthor("Smith, Donald")
            )))
        ;
    }

    @Test
    public void discoverFacetsAuthorTestWithPrefix_Capital_And_Special_Chars() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection").build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 1")
            .withIssueDate("2017-10-17")
            .withAuthor("Smith, John")
            .withAuthor("Jan, Doe")
            .build();

        Item publicItem2 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 2")
            .withIssueDate("2016-02-13")
            .withAuthor("S’Idan, Mo")
            .withAuthor("Tick&Tock")
            .build();

        Item publicItem3 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 3")
            .withIssueDate("2016-02-13")
            .withAuthor("M Akai")
            .withAuthor("stIjn, SmITH")
            .build();

        Item publicItem4 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 4")
            .withIssueDate("2012-05-13")
            .withSubject("St Augustine")
            .build();

        Item publicItem5 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 5")
            .withIssueDate("2015-11-23")
            .withSubject("Health & Medicine")
            .build();

        Item publicItem6 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 6")
            .withIssueDate("2003-07-11")
            .withSubject("1% economy")
            .build();

        Item publicItem7 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 7")
            .withIssueDate("2008-12-31")
            .withSubject("I.T.")
            .build();

        Item publicItem8 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item 8")
            .withIssueDate("2013-07-21")
            .withSubject("?Unknown")
            .build();

        context.restoreAuthSystemState();

        // The prefix query for author queries should be case-insensitive and correctly handle special characters

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "Smith")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entryFacetWithoutSelfLink("Smith, John"),
                FacetValueMatcher.entryFacetWithoutSelfLink("stIjn, SmITH"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "S")
                .param("embed", "values"))
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher
                        .entryFacetWithoutSelfLink("Smith, John"),
                    FacetValueMatcher
                        .entryFacetWithoutSelfLink("S’Idan, Mo"),
                    // gets returned once for smith, once for stijn
                    FacetValueMatcher
                        .entryFacetWithoutSelfLink("stIjn, SmITH"),
                    FacetValueMatcher
                        .entryFacetWithoutSelfLink("stIjn, SmITH"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "M A")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("M Akai"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "S’I")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("S’Idan, Mo"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "Jan, D")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("Jan, Doe"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "Tick&")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("Tick&Tock"))));

        // Should also be the case for subject queries

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "St A")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher
                    .entryFacetWithoutSelfLink("St Augustine"))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Health & M")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher
                    .entryFacetWithoutSelfLink("Health & Medicine"))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "1% e")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("1% economy"))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "I.")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("I.T."))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "U")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(FacetValueMatcher.entryFacetWithoutSelfLink("?Unknown"))));
    }

    @Test
    public void discoverFacetsAuthorTestWithPrefixFirstName() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Parent Collection").build();

        Item item1 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 1")
            .withAuthor("Smith, John")
            .build();

        Item item2 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 2")
            .withAuthor("Smith, Jane")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "john")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(
                    FacetValueMatcher.entryAuthor("Smith, John"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "jane")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(
                    FacetValueMatcher.entryAuthor("Smith, Jane"))));

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "j")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(
                    FacetValueMatcher.entryAuthor("Smith, John"),
                    FacetValueMatcher.entryAuthor("Smith, Jane"))));
    }

    @Test
    public void discoverFacetsAuthorWithAuthorityTestWithPrefixFirstName() throws Exception {
        configurationService.setProperty("choices.plugin.dc.contributor.author", "SolrAuthorAuthority");
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");

        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Parent Collection").build();

        Item item1 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 1")
            .withAuthor("Smith, John", "test_authority_1", Choices.CF_ACCEPTED)
            .build();

        Item item2 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 2")
            .withAuthor("Smith, Jane", "test_authority_2", Choices.CF_ACCEPTED)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/author")
                .param("prefix", "j")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values",
                containsInAnyOrder(
                    FacetValueMatcher.entryAuthorWithAuthority(
                        "Smith, John", "test_authority_1", 1),
                    FacetValueMatcher.entryAuthorWithAuthority(
                        "Smith, Jane", "test_authority_2", 1))));

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();

        metadataAuthorityService.clearCache();
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
        getClient().perform(get("/api/discover/facets/author").param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be author, because that's the facet that we called upon
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' because that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", is("text")))
            //There always needs to be a self link present
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
            //The page object needs to present and exactly like how it is specified here. 20 is entered as the
            // size because that's the default in the configuration if no size parameter has been given
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 20))))
            //The authors need to be embedded in the values, all 4 of them have to be present as the size
            // allows it
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("page", "1")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name of the facet has to be author as that's the one we called
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' as this is the default configuration
            .andExpect(jsonPath("$.facetType", is("text")))
            //There needs to be a next link because there are more authors than the current size is allowed to
            // show. There are more pages after this one
            .andExpect(jsonPath("$._embedded.values._links.next.href",
                containsString("api/discover/facets/author")))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
            //The page object has to be like this because that's what we've asked in the parameters
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(1, 2))))
            //These authors have to be present because of the current configuration
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("f.title", "test,contains")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be author as that's the facet that we've asked
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType needs to be 'text' as that's the default configuration for the given facet
            .andExpect(jsonPath("$.facetType", is("text")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author")))
            //The self link needs to contain the query that was specified in the parameters, this is how it
            // looks like
            .andExpect(jsonPath("$._links.self.href", containsString("f.title=test,contains")))
            //This is how the page object must look like because it's the default
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 20))))
            //These authors need to be present in the result because these have made items that contain 'test'
            // in the title
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entryAuthor("Smith, Donald"),
                FacetValueMatcher.entryAuthor("Testing, Works")
            )))
            //These authors cannot be present because they've not produced an item with 'test' in the title
            .andExpect(jsonPath("$._embedded.values._embedded.values", not(containsInAnyOrder(
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
        getClient().perform(get("/api/discover/facets/dateIssued").param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be 'dateIssued' as that's the facet that we've called
            .andExpect(jsonPath("$.name", is("dateIssued")))
            //The facetType has to be 'date' because that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", is("date")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
            //This is how the page object must look like because it's the default with size 20
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 20))))
            //The date values need to be as specified below
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("scope", "testScope")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be author as that's the facet that we've called
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' as that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", is("text")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href",
                containsString("api/discover/facets/author?scope=testScope")))
            //These are all the authors for the items that were created and thus they have to be present in
            // the embedded values section
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("size", "2")
                .param("embed", "values"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be 'author' as that's the facet that we called
            .andExpect(jsonPath("$.name", is("author")))
            //The facetType has to be 'text' as that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", is("text")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href",
                containsString("api/discover/facets/author?scope=testScope")))
            .andExpect(jsonPath("$._embedded.values._links.next.href",
                containsString("api/discover/facets/author/values?scope=testScope&page=1&size=2")))
            //These are the values that need to be present as it's ordered by count and these authors are the
            // most common ones in the items that we've created
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("size", "2")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name needs to be dateIssued as that's the facet that we've called
            .andExpect(jsonPath("$.name", is("dateIssued")))
            //the facetType needs to be 'date' as that's the default facetType for this facet in the
            // configuration
            .andExpect(jsonPath("$.facetType", is("date")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
            //Seeing as we've entered a size of two and there are more dates than just two, we'll need a next
            // link to go to the next page to see the rest of the dates
            .andExpect(jsonPath("$._embedded.values._links.next.href",
                containsString("api/discover/facets/dateIssued")))
            //The page object needs to look like this because we've entered a size of 2 and we didn't specify
            // a starting page so it defaults to 0
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 2))))
            //There needs to be two date results in the embedded values section because that's what we've
            // specified
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
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
                .param("size", "2")
                .param("embed", "values"))

            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be dateIssued because that's the facet that we called
            .andExpect(jsonPath("$.name", is("dateIssued")))
            //The facetType needs to be 'date' as that's the default facetType for this facet in the
            // configuration
            .andExpect(jsonPath("$.facetType", is("date")))
            //There always needs to be a self link
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/dateIssued")))
            //The page object needs to look like this because we entered a size of 2 and we didn't specify a
            // starting page so it defaults to 0
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 2))))
            //There needs to be only two date intervals with a count of 1 because of the query we specified
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entryDateIssuedWithCountOne(),
                FacetValueMatcher.entryDateIssuedWithCountOne()
            )))
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
        //An anonymous user browses this endpoint to find the objects in the system
        //With a size 2
        getClient().perform(get("/api/discover/facets")
                .param("configuration", "minAndMaxTests"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.facets", Matchers.containsInAnyOrder(
                FacetEntryMatcher.authorFacetWithMinMax("Doe, Jane", "Testing, Works"),
                FacetEntryMatcher.entityTypeFacet(),
                FacetEntryMatcher.subjectFacet(),
                FacetEntryMatcher.dateIssuedFacetWithMinMax("1990-02-13", "2010-10-17"),
                FacetEntryMatcher.hasContentInOriginalBundleFacet()
            )))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href", containsString("/api/discover/facets")))
        ;

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
                .param("size", "10")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self.href",containsString(
                "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC")))
            .andExpect(jsonPath("$._embedded.values._embedded.values", Matchers.containsInAnyOrder(
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
                .param("size", "1")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._links.self.href",containsString(
                "/api/discover/facets/discoverable?configuration=administrativeView&sort=score,DESC")))
            .andExpect(jsonPath("$._embedded.values._links.next.href",containsString(
                "/api/discover/facets/discoverable/values?configuration=administrativeView&sort=score,DESC&page=1&size=1")))
            .andExpect(jsonPath("$._embedded.values._embedded.values", Matchers.contains(
                SearchResultMatcher.matchEmbeddedFacetValues("true", 2, "discover",
                    "/api/discover/search/objects?configuration=administrativeView&f.discoverable=true,equals")
            )));

        getClient(adminToken).perform(get("/api/discover/facets/discoverable")
                .param("configuration", "administrativeView")
                .param("sort", "score,DESC")
                .param("page", "1")
                .param("size", "1")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._links.first.href",containsString(
                "/api/discover/facets/discoverable/values?configuration=administrativeView&sort=score,DESC&page=0&size=1")))
            .andExpect(jsonPath("$._embedded.values._links.prev.href",containsString(
                "/api/discover/facets/discoverable/values?configuration=administrativeView&sort=score,DESC&page=0&size=1")))
            .andExpect(jsonPath("$._embedded.values._links.self.href",containsString(
                "/api/discover/facets/discoverable/values?configuration=administrativeView&sort=score,DESC&page=1&size=1")))
            .andExpect(jsonPath("$._embedded.values._embedded.values", Matchers.contains(
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
                .param("query", "Donald")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("facet")))
            .andExpect(jsonPath("$.name", is("author")))
            .andExpect(jsonPath("$.facetType", is("text")))
            .andExpect(jsonPath("$._links.self.href", containsString("api/discover/facets/author?query=Donald")))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0].label", is("Smith, Donald")))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0].count", is(1)))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0]._links.search.href",
                containsString("api/discover/search/objects?query=Donald&f.author=" +
                    urlPathSegmentEscaper().escape("Smith, Donald,equals")
                )))
            .andExpect(jsonPath("$._embedded.values._embedded.values").value(Matchers.hasSize(1)));

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
                .param("dsoType", "Item")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("facet")))
            .andExpect(jsonPath("$.name", is("dateIssued")))
            .andExpect(jsonPath("$.facetType", is("date")))
            .andExpect(jsonPath("$._links.self.href",
                containsString("api/discover/facets/dateIssued?dsoType=Item")))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0].label", is("2017 - 2020")))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0].count", is(3)))
            .andExpect(jsonPath("$._embedded.values._embedded.values[0]._links.search.href",
                containsString("api/discover/search/objects?dsoType=Item&f.dateIssued=" +
                    urlPathSegmentEscaper().escape("[2017 TO 2020],equals")
                )))
            .andExpect(jsonPath("$._embedded.values._embedded.values").value(Matchers.hasSize(1)));

    }

    @Test
    public void discoverFacetsSubjectTestWithCapitalAndSpecialChars() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Parent Collection").build();

        Item item1 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 1")
            .withSubject("Value with: Multiple Words ")
            .build();

        Item item2 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 2")
            .withSubject("Multiple worded subject ")
            .build();

        Item item3 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 3")
            .withSubject("Subject with a lot of Word values")
            .build();

        Item item4 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 4")
            .withSubject("With, Values")
            .build();

        Item item5 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 5")
            .withSubject("Test:of:the:colon")
            .build();

        Item item6 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 6")
            .withSubject("Test,of,comma")
            .build();

        Item item7 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 7")
            .withSubject("N’guyen")
            .build();

        Item item8 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 8")
            .withSubject("test;Semicolon")
            .build();

        Item item9 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 9")
            .withSubject("test||of|Pipe")
            .build();

        Item item10 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 10")
            .withSubject("Test-Subject")
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "with a lot of word")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Subject with a lot of Word values", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "multiple words")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Value with: Multiple Words", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "mUltiPle wor")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Multiple worded subject", 1),
                FacetValueMatcher.entrySubject("Value with: Multiple Words", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "with")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("With, Values", 1),
                FacetValueMatcher.entrySubject("Subject with a lot of Word values", 1),
                FacetValueMatcher.entrySubject("Value with: Multiple Words", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "of")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Subject with a lot of Word values", 1),
                FacetValueMatcher.entrySubject("Test,of,comma", 1),
                FacetValueMatcher.entrySubject("Test:of:the:colon", 1),
                FacetValueMatcher.entrySubject("test||of|Pipe", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "tEsT")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Test,of,comma", 1),
                FacetValueMatcher.entrySubject("Test-Subject", 1),
                FacetValueMatcher.entrySubject("Test:of:the:colon", 1),
                FacetValueMatcher.entrySubject("test;Semicolon", 1),
                FacetValueMatcher.entrySubject("test||of|Pipe", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "colon")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Test:of:the:colon", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "coMma")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Test,of,comma", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "guyen")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("N’guyen", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "semiColon")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("test;Semicolon", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "pipe")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("test||of|Pipe", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Subject")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubject("Multiple worded subject", 1),
                FacetValueMatcher.entrySubject("Test-Subject", 1),
                FacetValueMatcher.entrySubject("Subject with a lot of Word values", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Subject of word")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values").isEmpty())
            .andExpect(jsonPath("$._embedded.values.page.number", is(0)));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Value with words")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values").isEmpty())
            .andExpect(jsonPath("$._embedded.values.page.number", is(0)));
    }

    @Test
    public void discoverFacetsSubjectWithAuthorityTest() throws Exception {
        configurationService.setProperty("choices.plugin.dc.subject", "SolrSubjectAuthority");
        configurationService.setProperty("authority.controlled.dc.subject", "true");

        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community").build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Parent Collection").build();

        Item item1 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 1")
            .withSubject("Value with: Multiple Words",
                "test_authority_1", Choices.CF_ACCEPTED)
            .build();

        Item item2 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 2")
            .withSubject("Multiple worded subject ",
                "test_authority_2", Choices.CF_ACCEPTED)
            .build();

        Item item3 = ItemBuilder.createItem(context, collection)
            .withTitle("Item 3")
            .withSubject("Subject with a lot of Word values",
                "test_authority_3", Choices.CF_ACCEPTED)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "with a lot of word")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubjectWithAuthority("Subject with a lot of Word values",
                    "test_authority_3", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "mUltiPle wor")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubjectWithAuthority("Multiple worded subject",
                    "test_authority_2", 1),
                FacetValueMatcher.entrySubjectWithAuthority("Value with: Multiple Words",
                    "test_authority_1", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Subject")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                FacetValueMatcher.entrySubjectWithAuthority("Multiple worded subject",
                    "test_authority_2", 1),
                FacetValueMatcher.entrySubjectWithAuthority("Subject with a lot of Word values",
                    "test_authority_3", 1))));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Subject of word")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values").isEmpty())
            .andExpect(jsonPath("$._embedded.values.page.number", is(0)));

        getClient().perform(get("/api/discover/facets/subject")
                .param("prefix", "Value with words")
                .param("embed", "values"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.values._embedded.values").isEmpty())
            .andExpect(jsonPath("$._embedded.values.page.number", is(0)));

        DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();

        metadataAuthorityService.clearCache();
    }

    @Test
    public void discoverFacetsSupervisedByTest() throws Exception {
        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collection
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        //2. Two workspace items
        WorkspaceItem wsItem1 =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item 1")
                .withIssueDate("2010-07-23")
                .build();

        WorkspaceItem wsItem2 =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item 2")
                .withIssueDate("2010-11-03")
                .build();

        //3. Two groups
        Group groupA =
            GroupBuilder.createGroup(context)
                .withName("group A")
                .addMember(eperson)
                .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                .withName("group B")
                .addMember(eperson)
                .build();

        //4. Four supervision orders
        SupervisionOrder supervisionOrderOne =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1.getItem(), groupA).build();

        SupervisionOrder supervisionOrderTwo =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1.getItem(), groupB).build();

        SupervisionOrder supervisionOrderThree =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2.getItem(), groupA).build();

        SupervisionOrder supervisionOrderFour =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2.getItem(), groupB).build();

        context.restoreAuthSystemState();

        //** WHEN **
        //The Admin user browses this endpoint to find the supervisedBy results by the facet
        getClient(getAuthToken(admin.getEmail(), password)).perform(get("/api/discover/facets/supervisedBy")
                .param("configuration", "supervision")
                .param("embed", "values"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be 'supervisedBy' as that's the facet that we've called
            .andExpect(jsonPath("$.name", is("supervisedBy")))
            //The facetType has to be `authority` because that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", equalTo("authority")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href",
                containsString("api/discover/facets/supervisedBy?configuration=supervision")))
            //This is how the page object must look like because it's the default with size 20
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 20))))
            //The supervisedBy values need to be as specified below
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                entrySupervisedBy(groupA.getName(), groupA.getID().toString(), 2),
                entrySupervisedBy(groupB.getName(), groupB.getID().toString(), 2)
            )));
    }

    @Test
    public void discoverFacetsSupervisedByWithPrefixTest() throws Exception {
        //We turn off the authorization system in order to create the structure defined below
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collection
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection collection =
            CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection 1")
                .build();

        //2. Two workspace items
        WorkspaceItem wsItem1 =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item 1")
                .withIssueDate("2010-07-23")
                .build();

        WorkspaceItem wsItem2 =
            WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle("Workspace Item 2")
                .withIssueDate("2010-11-03")
                .build();

        Group groupA =
            GroupBuilder.createGroup(context)
                .withName("group A")
                .addMember(eperson)
                .build();

        Group groupB =
            GroupBuilder.createGroup(context)
                .withName("group B")
                .addMember(eperson)
                .build();

        SupervisionOrder supervisionOrderOneA =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1.getItem(), groupA).build();

        SupervisionOrder supervisionOrderOneB =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem1.getItem(), groupB).build();

        SupervisionOrder supervisionOrderTwoA =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2.getItem(), groupA).build();

        SupervisionOrder supervisionOrderTwoB =
            SupervisionOrderBuilder.createSupervisionOrder(context, wsItem2.getItem(), groupB).build();

        context.restoreAuthSystemState();

        //** WHEN **
        //The Admin user browses this endpoint to find the supervisedBy results by the facet
        getClient(getAuthToken(admin.getEmail(), password)).perform(get("/api/discover/facets/supervisedBy")
                .param("configuration", "supervision")
                .param("prefix", "group B")
                .param("embed", "values"))
            //** THEN **
            //The status has to be 200 OK
            .andExpect(status().isOk())
            //The type has to be 'discover'
            .andExpect(jsonPath("$.type", is("facet")))
            //The name has to be 'supervisedBy' as that's the facet that we've called
            .andExpect(jsonPath("$.name", is("supervisedBy")))
            //The facetType has to be `authority` because that's the default configuration for this facet
            .andExpect(jsonPath("$.facetType", equalTo("authority")))
            //There always needs to be a self link available
            .andExpect(jsonPath("$._links.self.href",
                containsString("api/discover/facets/supervisedBy?prefix=group%20B&configuration=supervision")))
            //This is how the page object must look like because it's the default with size 20
            .andExpect(jsonPath("$._embedded.values.page",
                is(PageMatcher.pageEntry(0, 20))))
            //The supervisedBy values need to be as specified below
            .andExpect(jsonPath("$._embedded.values._embedded.values", containsInAnyOrder(
                entrySupervisedBy(groupB.getName(), groupB.getID().toString(), 2)
            )));
    }
}
