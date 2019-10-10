/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.model.PageRest;
import org.dspace.app.rest.model.SearchEventRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.junit.Test;

public class SearchEventRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTestThrowNotImplementedException() throws Exception {

        getClient().perform(get("/api/statistics/searchevents"))
                   .andExpect(status().is(405));
    }
    @Test
    public void findOneTestThrowNotImplementedException() throws Exception {

        getClient().perform(get("/api/statistics/searchevents/" + UUID.randomUUID()))
                   .andExpect(status().is(405));
    }

    @Test
    public void postTestSucces() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "desc");
        searchEventRest.setSort(sort);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postTestNullPageBadRequestException() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "desc");
        searchEventRest.setSort(sort);

        searchEventRest.setPage(null);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postTestNullSortBadRequestException() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        searchEventRest.setSort(null);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postTestSortInvalidOrderBadRequestException() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "azertre");
        searchEventRest.setSort(sort);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isBadRequest());

    }

    @Test
    public void postTestSuccesUppercaseSortOrder() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "DESC");
        searchEventRest.setSort(sort);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postTestSuccesNoAppliedFilters() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setQuery("test");
        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "desc");
        searchEventRest.setSort(sort);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }

    @Test
    public void postTestSuccesEmptyQuery() throws Exception {

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

        context.restoreAuthSystemState();

        SearchEventRest searchEventRest = new SearchEventRest();

        searchEventRest.setScope(publicItem1.getID());
        searchEventRest.setConfiguration("default");
        searchEventRest.setDsoType("item");

        SearchResultsRest.Sorting sort = new SearchResultsRest.Sorting("title", "desc");
        searchEventRest.setSort(sort);

        PageRest pageRest = new PageRest(5, 20, 4, 1);
        searchEventRest.setPage(pageRest);

        SearchResultsRest.AppliedFilter appliedFilter =
            new SearchResultsRest.AppliedFilter("author", "contains", "test","test");
        List<SearchResultsRest.AppliedFilter> appliedFilterList = new LinkedList<>();
        appliedFilterList.add(appliedFilter);
        searchEventRest.setAppliedFilters(appliedFilterList);

        ObjectMapper mapper = new ObjectMapper();

        getClient().perform(post("/api/statistics/searchevents")
                                .content(mapper.writeValueAsBytes(searchEventRest))
                                .contentType(contentType))
                   .andExpect(status().isCreated());

    }
}
