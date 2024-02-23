/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.ContentReportMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.model.FilteredCollectionsQuery;
import org.dspace.app.rest.model.FilteredItemsQueryPredicate;
import org.dspace.app.rest.model.FilteredItemsQueryRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.contentreport.Filter;
import org.dspace.contentreport.FilteredCollection;
import org.dspace.contentreport.QueryOperator;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the content reports ported from DSpace 6.x
 * (Filtered Collections and Filtered Items).
 * @author Jean-François Morin (Université Laval)
 */
public class ContentReportRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void testFilteredCollections() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("contentreport.enable", Boolean.TRUE);

        //** GIVEN **
        //1. A community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("My Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(10);
        String fmtPastDate = DateTimeFormatter.ISO_DATE.format(pastDate);
        LocalDate futureDate = today.plusDays(10);
        String fmtFutureDate = DateTimeFormatter.ISO_DATE.format(futureDate);

        //2. Three items, two of which are available and discoverable...
        ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate(fmtPastDate)
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate(fmtPastDate)
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        // ... and one will be available in a few days.
        ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate(fmtFutureDate)
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        Map<Filter, Integer> valuesCol1 = Map.of(Filter.IS_DISCOVERABLE, 1);
        FilteredCollection fcol1 = FilteredCollection.of(col1.getName(), col1.getHandle(),
                parentCommunity.getName(), parentCommunity.getHandle(),
                1, 1, valuesCol1, true);
        Map<Filter, Integer> valuesCol2 = Map.of(Filter.IS_DISCOVERABLE, 2);
        FilteredCollection fcol2 = FilteredCollection.of(col2.getName(), col2.getHandle(),
                parentCommunity.getName(), parentCommunity.getHandle(),
                2, 2, valuesCol2, true);

        FilteredCollectionsQuery query = FilteredCollectionsQuery.of(Set.of(Filter.IS_DISCOVERABLE));

        getClient(token).perform(get("/api/contentreport/filteredcollections?" + query.toQueryString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.collections", Matchers.containsInAnyOrder(
                           ContentReportMatcher.matchFilteredCollectionProperties(fcol1),
                           ContentReportMatcher.matchFilteredCollectionProperties(fcol2)
                   )))
                   .andExpect(jsonPath("type", is("filteredcollectionsreport")))
                   .andExpect(jsonPath("$.summary",
                           ContentReportMatcher.matchFilteredCollectionSummary(3, 3)))
                   .andExpect(jsonPath("$._links.self.href",
                           Matchers.containsString("/api/contentreport/filteredcollections")));
    }

    @Test
    public void testFilteredCollectionsOff() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("contentreport.enable", Boolean.FALSE);

        //** GIVEN **
        //1. A community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("My Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        LocalDate today = LocalDate.now();
        LocalDate pastDate = today.minusDays(10);
        String fmtPastDate = DateTimeFormatter.ISO_DATE.format(pastDate);
        LocalDate futureDate = today.plusDays(10);
        String fmtFutureDate = DateTimeFormatter.ISO_DATE.format(futureDate);

        //2. Three items, two of which are available and discoverable...
        ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate(fmtPastDate)
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate(fmtPastDate)
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        // ... and one will be available in a few days.
        ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate(fmtFutureDate)
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        FilteredCollectionsQuery query = FilteredCollectionsQuery.of(Set.of(Filter.IS_DISCOVERABLE));

        getClient(token).perform(get("/api/contentreport/filteredcollections?" + query.toQueryString()))
                   .andExpect(status().isNotFound());
    }

    // This test is disabled until someone can find out why the search function doesn't behave properly
    // in the present test context while it does when used against a standard production database.
    @Ignore
    @Test
    public void testFilteredItems() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("contentreport.enable", Boolean.TRUE);

        //** GIVEN **
        //1. A community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("My Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        List<Item> items = new ArrayList<>();

        //2. Three items, two of which are available and discoverable...
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem1);

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem2);

        // ... and one will be available in a few days.
        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem3);

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        FilteredItemsQueryRest query = FilteredItemsQueryRest.of(null,
                List.of(FilteredItemsQueryPredicate.of("dc.contributor.author", QueryOperator.EQUALS, "Doe, Jane")),
                100, null, List.of("dc.subject"));

        ObjectMapper mapper = new ObjectMapper();

        getClient(token).perform(get("/api/contentreport/filtereditems?" + query.toQueryString())
                .content(mapper.writeValueAsBytes(query))
                .contentType(contentType))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
                .andExpect(jsonPath("$.itemCount", is(2)))
                .andExpect(jsonPath("$.items", Matchers.containsInAnyOrder(
                        ItemMatcher.matchItemProperties(publicItem2),
                        ItemMatcher.matchItemProperties(publicItem3)
                )));
    }

    @Test
    public void testFilteredItemsOff() throws Exception {
        context.turnOffAuthorisationSystem();

        configurationService.setProperty("contentreport.enable", Boolean.FALSE);

        //** GIVEN **
        //1. A community with two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("My Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 2").build();

        List<Item> items = new ArrayList<>();

        //2. Three items, two of which are available and discoverable...
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem1);

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem2);

        // ... and one will be available in a few days.
        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();
        items.add(publicItem3);

        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);

        FilteredItemsQueryRest query = FilteredItemsQueryRest.of(null,
                List.of(FilteredItemsQueryPredicate.of("dc.contributor.author", QueryOperator.EQUALS, "Doe, Jane")),
                100, null, List.of("dc.subject"));

        getClient(token).perform(get("/api/contentreport/filtereditems?" + query.toQueryString()))
                .andExpect(status().isNotFound());
    }

}
