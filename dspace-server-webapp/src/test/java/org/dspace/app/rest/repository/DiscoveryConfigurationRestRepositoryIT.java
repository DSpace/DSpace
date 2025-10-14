/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.DiscoveryConfigurationMatcher;
import org.dspace.app.rest.matcher.SearchFilterMatcher;
import org.dspace.app.rest.matcher.SortOptionMatcher;
import org.dspace.app.rest.model.DiscoveryConfigurationRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.junit.Before;
import org.junit.Test;

public class DiscoveryConfigurationRestRepositoryIT extends AbstractControllerIntegrationTest {


    Community community;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();

        // add a unique handle to use as scope later in the tests
        community = CommunityBuilder.createCommunity(context, "123456789/unique-handle-here").build();

        context.commit();
        context.restoreAuthSystemState();
    }


    /**
     * Test that the endpoint can find different discovery configurations based on their names
     */
    @Test
    public void testFindDiscoveryConfigurationByName() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "default";
        DiscoveryConfiguration defaultConf = SearchUtils.getDiscoveryConfiguration(context, configName, null);


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultConf)));



        configName = "default-relationships";

        DiscoveryConfiguration defaultRelationshipsConf =
            SearchUtils.getDiscoveryConfiguration(context, configName, null);


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultRelationshipsConf)));

    }


    /**
     * Test that we can use a scope instead of a name to find a discovery configuration
     */
    @Test
    public void testFindDiscoveryConfigurationByUUIDScope() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        DiscoveryConfiguration NotifyOutgoingInvolvedItemsConfiguration =
            SearchUtils.getDiscoveryConfiguration(context, community);

        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, "scope"))
                .queryParam("uuid", community.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher
                .matchDiscoveryConfiguration(NotifyOutgoingInvolvedItemsConfiguration)));
    }


    /**
     * Test that the default configuration is used when a name is provided that doesn't resolve to any configuration
     */
    @Test
    public void testNonExistingName() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "does-not-exist";
        DiscoveryConfiguration defaultConf = SearchUtils.getDiscoveryConfiguration(context, "default", null);


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultConf)));
    }


    /**
     * Test that the default configuration is used when a scope that doesn't resolve to any configuration is provided
     */
    @Test
    public void testNonExistingScope() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        DiscoveryConfiguration defaultConf = SearchUtils.getDiscoveryConfiguration(context, "default", null);


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, "scope"))
                .queryParam("uuid", UUID.randomUUID().toString())) // random UUID
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultConf)));
    }


    /**
     * Test the functionality of the searchfilters followlink
     */
    @Test
    public void testFollowLinkSearchFilters() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "default";
        DiscoveryConfiguration defaultConf = SearchUtils.getDiscoveryConfiguration(context, "default", null);

        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultConf)))
            .andExpect(jsonPath("$._links.searchfilters", not(empty())));



        String followLink = String.format("/api/%s/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName, "searchfilters");

        getClient(adminToken).perform(get(followLink))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(defaultConf.getSearchFilters().size())))
            .andExpect(jsonPath("$._embedded.searchfilters",
                containsInAnyOrder(SearchFilterMatcher.createSearchFilterMatchers(defaultConf.getSearchFilters()))));



        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName))
                .queryParam("embed", "searchfilters"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.searchfilters.page.totalElements", is(defaultConf.getSearchFilters().size())))
            .andExpect(jsonPath("$._embedded.searchfilters._embedded.searchfilters",
                containsInAnyOrder(SearchFilterMatcher.createSearchFilterMatchers(defaultConf.getSearchFilters()))));
    }



    /**
     * Test the functionality of the sortoptions followlink
     */
    @Test
    public void testFollowLinkSortOptions() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "default";
        DiscoveryConfiguration defaultConf = SearchUtils.getDiscoveryConfiguration(context, "default", null);

        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(defaultConf)))
            .andExpect(jsonPath("$._links.sortoptions", not(empty())));


        String followLink = String.format("/api/%s/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName, "sortoptions");

        getClient(adminToken).perform(get(followLink))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(defaultConf.getSearchSortConfiguration().getSortFields().size())))
            .andExpect(jsonPath("$._embedded.sortoptions",
                containsInAnyOrder(SortOptionMatcher.createSortOptionMatchers(defaultConf.getSearchSortConfiguration().getSortFields()))));


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName))
                .queryParam("embed", "sortoptions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.sortoptions.page.totalElements", is(defaultConf.getSearchSortConfiguration().getSortFields().size())))
            .andExpect(jsonPath("$._embedded.sortoptions._embedded.sortoptions",
                containsInAnyOrder(SortOptionMatcher.createSortOptionMatchers(defaultConf.getSearchSortConfiguration().getSortFields()))));
    }




    /**
     * Test the functionality of the defaultSortOption followlink
     */
    @Test
    public void testFollowLinkDefaultSortOption() throws Exception {

        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "workspace";
        DiscoveryConfiguration workspaceConf = SearchUtils.getDiscoveryConfiguration(context, configName, null);

        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(workspaceConf)))
            .andExpect(jsonPath("$._links.defaultsortoption", not(empty())));


        String followLink = String.format("/api/%s/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
            DiscoveryConfigurationRest.PLURAL_NAME, configName, "defaultsortoption");

        getClient(adminToken).perform(get(followLink))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                SortOptionMatcher.matchSortFieldConfiguration(workspaceConf.getSearchSortConfiguration()
                .getDefaultSortField())));


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName))
                .queryParam("embed", "defaultsortoption"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.defaultsortoption",
                SortOptionMatcher.matchSortFieldConfiguration(workspaceConf.getSearchSortConfiguration()
                    .getDefaultSortField())));
    }


    /**
     * Test the endpoint still works as expected when projection full is provided as query parameter
     */
    @Test
    public void testFullProjection() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);

        String configName = "workspace";
        DiscoveryConfiguration workspaceConf =
            SearchUtils.getDiscoveryConfiguration(context, configName, null);


        getClient(adminToken).perform(get(String.format("/api/%s/%s/%s", DiscoveryConfigurationRest.CATEGORY,
                DiscoveryConfigurationRest.PLURAL_NAME, configName))
                .param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration(workspaceConf)))
            .andExpect(jsonPath("$._embedded.searchfilters._embedded.searchfilters",
                containsInAnyOrder(SearchFilterMatcher.createSearchFilterMatchers(workspaceConf.getSearchFilters()))))
            .andExpect(jsonPath("$._embedded.sortoptions._embedded.sortoptions",
                containsInAnyOrder(SortOptionMatcher.createSortOptionMatchers(
                    workspaceConf.getSearchSortConfiguration().getSortFields()))))
            .andExpect(jsonPath("$._embedded.defaultsortoption",
                SortOptionMatcher.matchSortFieldConfiguration(workspaceConf.getSearchSortConfiguration()
                    .getDefaultSortField())));

    }
}
