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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.DiscoveryConfigurationMatcher;
import org.dspace.app.rest.matcher.SearchFilterMatcher;
import org.dspace.app.rest.matcher.SortOptionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Community;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.junit.Before;
import org.junit.Test;

public class DiscoveryConfigurationRestRepositoryIT extends AbstractControllerIntegrationTest {

    private Community community;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        community = CommunityBuilder.createCommunity(context, "123456789/discovery-configuration-tests").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testFindOneByName() throws Exception {
        getClient().perform(get("/api/discover/discoveryconfigurations/default"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")));

        getClient().perform(get("/api/discover/discoveryconfigurations/workspace"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("workspace")));

        getClient().perform(get("/api/discover/discoveryconfigurations/workflow"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("workflow")));
    }

    @Test
    public void testFindOneByScope() throws Exception {
        getClient().perform(get("/api/discover/discoveryconfigurations/scope")
                                    .param("uuid", community.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher
                           .matchDiscoveryConfiguration("discovery-parent-community-1")));
    }

    @Test
    public void testFindOneByNameDefaultFallback() throws Exception {
        getClient().perform(get("/api/discover/discoveryconfigurations/does-not-exist"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")));
    }

    @Test
    public void testFindOneByScopeDefaultFallback() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community2 = CommunityBuilder.createCommunity(context).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/discover/discoveryconfigurations/scope")
                                    .param("uuid", community2.getID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")));

        getClient().perform(get("/api/discover/discoveryconfigurations/scope")
                                    .param("uuid", UUID.randomUUID().toString()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")));
    }

    @Test
    public void testLinkSearchFilters() throws Exception {
        DiscoveryConfiguration config = SearchUtils.getDiscoveryConfiguration(context, "default", null);

        getClient().perform(get("/api/discover/discoveryconfigurations/default"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")))
                   .andExpect(jsonPath("$._links.searchfilters", not(empty())));

        getClient().perform(get("/api/discover/discoveryconfigurations/default/searchfilters"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(config.getSearchFilters().size())))
                   .andExpect(jsonPath("$._embedded.searchfilters", containsInAnyOrder(
                           SearchFilterMatcher.createSearchFilterMatchers(config.getSearchFilters())
                   )));

        getClient().perform(get("/api/discover/discoveryconfigurations/default")
                                    .param("embed", "searchfilters"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.searchfilters.page.totalElements",
                                       is(config.getSearchFilters().size())))
                   .andExpect(jsonPath("$._embedded.searchfilters._embedded.searchfilters", containsInAnyOrder(
                           SearchFilterMatcher.createSearchFilterMatchers(config.getSearchFilters())
                   )));
    }

    @Test
    public void testLinkSortOptions() throws Exception {
        DiscoveryConfiguration config = SearchUtils.getDiscoveryConfiguration(context, "default", null);

        getClient().perform(get("/api/discover/discoveryconfigurations/default"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")))
                   .andExpect(jsonPath("$._links.sortoptions", not(empty())));

        getClient().perform(get("/api/discover/discoveryconfigurations/default/sortoptions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(config.getSearchSortConfiguration()
                                                                        .getSortFields().size())))
                   .andExpect(jsonPath("$._embedded.sortoptions", containsInAnyOrder(
                       SortOptionMatcher.createSortOptionMatchers(config.getSearchSortConfiguration().getSortFields())
                   )));

        getClient().perform(get("/api/discover/discoveryconfigurations/default")
                                    .param("embed", "sortoptions"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.sortoptions.page.totalElements",
                                       is(config.getSearchSortConfiguration().getSortFields().size())))
                   .andExpect(jsonPath("$._embedded.sortoptions._embedded.sortoptions", containsInAnyOrder(
                       SortOptionMatcher.createSortOptionMatchers(config.getSearchSortConfiguration().getSortFields())
                   )));
    }

    @Test
    public void testLinkDefaultSortOption() throws Exception {
        DiscoveryConfiguration config = SearchUtils.getDiscoveryConfiguration(context, "workspace", null);

        getClient().perform(get("/api/discover/discoveryconfigurations/workspace"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("workspace")))
                   .andExpect(jsonPath("$._links.defaultsortoption", not(empty())));

        getClient().perform(get("/api/discover/discoveryconfigurations/workspace/defaultsortoption"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", SortOptionMatcher.sortOptionMatcher(config.getSearchSortConfiguration()
                                                                                      .getDefaultSortField())
                   ));

        getClient().perform(get("/api/discover/discoveryconfigurations/workspace")
                                    .param("embed", "defaultsortoption"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.defaultsortoption",
                                       SortOptionMatcher.sortOptionMatcher(config.getSearchSortConfiguration()
                                                                                 .getDefaultSortField())));

        getClient().perform(get("/api/discover/discoveryconfigurations/default"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")))
                   .andExpect(jsonPath("$._links.defaultsortoption", not(empty())));

        getClient().perform(get("/api/discover/discoveryconfigurations/default/defaultsortoption"))
                   .andExpect(status().isNoContent());
    }

    @Test
    public void testFullProjection() throws Exception {
        DiscoveryConfiguration config = SearchUtils.getDiscoveryConfiguration(context, "default", null);

        getClient().perform(get("/api/discover/discoveryconfigurations/default")
                                    .param("projection", "full"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", DiscoveryConfigurationMatcher.matchDiscoveryConfiguration("default")))
                   .andExpect(jsonPath("$._embedded.searchfilters._embedded.searchfilters", containsInAnyOrder(
                           SearchFilterMatcher.createSearchFilterMatchers(config.getSearchFilters())
                   )))
                   .andExpect(jsonPath("$._embedded.sortoptions._embedded.sortoptions", containsInAnyOrder(
                           SortOptionMatcher.createSortOptionMatchers(config.getSearchSortConfiguration()
                                                                            .getSortFields()))))
                   .andExpect(jsonPath("$._embedded.defaultsortoption", nullValue()));
    }
}
