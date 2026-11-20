/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.matcher.StatisticsCategoryMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class StatisticsCategoryRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    private SiteService siteService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findAllTest() throws Exception {
        getClient().perform(get("/api/statistics/categories")).andExpect(status().isMethodNotAllowed());
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/statistics/categories")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void searchObjectDifferentTypesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Site site = siteService.findSite(context);
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //create collections
        Collection colPeople = CollectionBuilder.createCollection(context, parentCommunity).withName("People")
                .withEntityType("Person").build();
        Collection colPub = CollectionBuilder.createCollection(context, parentCommunity).withName("Publication")
                .withEntityType("Publication").build();
        Collection colProj = CollectionBuilder.createCollection(context, parentCommunity).withName("Project")
                .withEntityType("Project").build();
        Collection colOther = CollectionBuilder.createCollection(context, parentCommunity).withName("SomethingElse")
                .withEntityType("SomethingElse").build();
        Collection colUnspecified = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("A basic collection").build();
        // create items
        Item itemPers = ItemBuilder.createItem(context, colPeople).withTitle("Test Person item").build();
        Item itemPub = ItemBuilder.createItem(context, colPub).withTitle("Test Publication item").build();
        Item itemProj = ItemBuilder.createItem(context, colProj).withTitle("Test Project item").build();
        Item itemOther = ItemBuilder.createItem(context, colOther).withTitle("Test SomethingElse item").build();
        Item itemUnspecified = ItemBuilder.createItem(context, colUnspecified).withTitle("Test basic item").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/statistics/categories/search/object")
                .param("uri", "http://localhost:8080/server/api/items/" + itemPers.getID().toString())
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                    StatisticsCategoryMatcher.match("person-mainReports", "mainReports"),
                    StatisticsCategoryMatcher.match("person-publicationsReports", "publicationsReports"),
                    StatisticsCategoryMatcher.match("person-projectsReports", "projectsReports")
                    )));
        getClient(authToken).perform(get("/api/statistics/categories/search/object")
                .param("uri", "http://localhost:8080/server/api/items/" + itemPub.getID().toString())
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                    StatisticsCategoryMatcher.match("publication-mainReports", "mainReports")
                    )));
        getClient(authToken).perform(get("/api/statistics/categories/search/object")
                .param("uri", "http://localhost:8080/server/api/items/" + itemProj.getID().toString())
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                    StatisticsCategoryMatcher.match("project-mainReports", "mainReports"),
                    StatisticsCategoryMatcher.match("project-publicationsReports", "publicationsReports")
                    )));
        getClient(authToken).perform(get("/api/statistics/categories/search/object")
                .param("uri", "http://localhost:8080/server/api/items/" + itemOther.getID().toString())
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                    StatisticsCategoryMatcher.match("item-mainReports", "mainReports")
                    )));
        getClient(authToken).perform(get("/api/statistics/categories/search/object")
                .param("uri", "http://localhost:8080/server/api/items/" + itemUnspecified.getID().toString())
                )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 1)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                    StatisticsCategoryMatcher.match("item-mainReports", "mainReports")
                    )));
        //TODO test for categories with some empty reports
        //TODO test for categories with all empty reports
        //TODO test for categories with some not accessible reports
        //TODO test for categories with all not accessible reports
        //TODO test for categories with all not accessible or empty reports
    }

    @Test
    public void findOneTest() throws Exception {
        getClient().perform(get("/api/statistics/categories/site-mainReports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", Matchers.is("site-mainReports")))
                .andExpect(jsonPath("$.category-type", Matchers.is("mainReports")));
        getClient().perform(get("/api/statistics/categories/item-mainReports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", Matchers.is("item-mainReports")))
                .andExpect(jsonPath("$.category-type", Matchers.is("mainReports")));
    }

    @Test
    public void searchObjectNotWithoutAdminRestrictionTest() throws Exception {
        configurationService.setProperty("usage-statistics.authorization.admin.usage", false);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        //create collection
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        //create collections
        Collection colPeople = CollectionBuilder.createCollection(context, parentCommunity).withName("People")
            .withEntityType("Person").build();
        Item itemPers = ItemBuilder.createItem(context, colPeople).withTitle("Test Person item").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/statistics/categories/search/object")
            .param("uri", "http://localhost:8080/server/api/items/" + itemPers.getID().toString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 3)))
            .andExpect(jsonPath("$._embedded.categories", Matchers.contains(
                StatisticsCategoryMatcher.match("person-mainReports", "mainReports"),
                StatisticsCategoryMatcher.match("person-publicationsReports", "publicationsReports"),
                StatisticsCategoryMatcher.match("person-projectsReports", "projectsReports")
            )));
        configurationService.setProperty("usage-statistics.authorization.admin.usage", true);
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        getClient().perform(get("/api/statistics/categories/notexistings"))
                .andExpect(status().isNotFound());
        getClient().perform(get("/api/statistics/categories/3"))
                .andExpect(status().isNotFound());
    }

}
