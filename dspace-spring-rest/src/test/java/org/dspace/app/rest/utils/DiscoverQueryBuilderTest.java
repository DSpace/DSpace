/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.exception.InvalidDSpaceObjectTypeException;
import org.dspace.app.rest.exception.InvalidSearchFacetException;
import org.dspace.app.rest.exception.InvalidSearchFilterException;
import org.dspace.app.rest.exception.InvalidSortingException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.matchers.apachecommons.ReflectionEquals;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link DiscoverQueryBuilder}
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoverQueryBuilderTest {

    @InjectMocks
    private DiscoverQueryBuilder queryBuilder;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private SolrServiceImpl searchService;

    @Mock
    private Context context;

    @Mock
    private DSpaceObject scope;

    private DiscoveryConfiguration discoveryConfiguration;
    private String query;
    private SearchFilter searchFilter;
    private PageRequest page;

    @Before
    public void setUp() throws Exception {
        when(configurationService.getIntProperty(eq("rest.search.max.results"), anyInt())).thenReturn(100);

        when(searchService.escapeQueryChars(any(String.class))).then(invocation -> invocation.getArguments()[0]);

        when(searchService.toSortFieldIndex(any(String.class), any(String.class))).then(invocation -> invocation.getArguments()[0] + "_sort");

        when(searchService.getFacetYearRange(eq(context), any(DSpaceObject.class), any(DiscoverySearchFilterFacet.class), any())).then(invocation
                -> new FacetYearRange((DiscoverySearchFilterFacet) invocation.getArguments()[2]));

        when(searchService.toFilterQuery(any(Context.class), any(String.class), any(String.class), any(String.class)))
                .then(invocation -> new DiscoverFilterQuery((String) invocation.getArguments()[1],
                        invocation.getArguments()[1] + ":\"" + invocation.getArguments()[3] + "\"",
                        (String) invocation.getArguments()[3]));

        discoveryConfiguration = new DiscoveryConfiguration();
        discoveryConfiguration.setDefaultFilterQueries(Arrays.asList("archived:true"));


        DiscoveryHitHighlightingConfiguration discoveryHitHighlightingConfiguration = new DiscoveryHitHighlightingConfiguration();
        List<DiscoveryHitHighlightFieldConfiguration> discoveryHitHighlightFieldConfigurations = new LinkedList<>();

        DiscoveryHitHighlightFieldConfiguration discoveryHitHighlightFieldConfiguration = new DiscoveryHitHighlightFieldConfiguration();
        discoveryHitHighlightFieldConfiguration.setField("dc.title");

        DiscoveryHitHighlightFieldConfiguration discoveryHitHighlightFieldConfiguration1 = new DiscoveryHitHighlightFieldConfiguration();
        discoveryHitHighlightFieldConfiguration1.setField("fulltext");

        discoveryHitHighlightFieldConfigurations.add(discoveryHitHighlightFieldConfiguration1);
        discoveryHitHighlightFieldConfigurations.add(discoveryHitHighlightFieldConfiguration);

        discoveryHitHighlightingConfiguration.setMetadataFields(discoveryHitHighlightFieldConfigurations);
        discoveryConfiguration.setHitHighlightingConfiguration(discoveryHitHighlightingConfiguration);


        DiscoverySortConfiguration sortConfiguration = new DiscoverySortConfiguration();

        DiscoverySortFieldConfiguration defaultSort = new DiscoverySortFieldConfiguration();
        defaultSort.setMetadataField("dc.date.accessioned");
        defaultSort.setType(DiscoveryConfigurationParameters.TYPE_DATE);
        sortConfiguration.setDefaultSort(defaultSort);
        sortConfiguration.setDefaultSortOrder(DiscoverySortConfiguration.SORT_ORDER.desc);

        DiscoverySortFieldConfiguration titleSort = new DiscoverySortFieldConfiguration();
        titleSort.setMetadataField("dc.title");
        sortConfiguration.setSortFields(Arrays.asList(titleSort));

        discoveryConfiguration.setSearchSortConfiguration(sortConfiguration);

        DiscoverySearchFilterFacet subjectFacet = new DiscoverySearchFilterFacet();
        subjectFacet.setIndexFieldName("subject");
        subjectFacet.setFacetLimit(5);
        DiscoverySearchFilterFacet dateFacet = new DiscoverySearchFilterFacet();
        dateFacet.setIndexFieldName("dateIssued");
        dateFacet.setType(DiscoveryConfigurationParameters.TYPE_DATE);
        dateFacet.setFacetLimit(6);
        HierarchicalSidebarFacetConfiguration hierarchyFacet = new HierarchicalSidebarFacetConfiguration();
        hierarchyFacet.setIndexFieldName("hierarchy");
        hierarchyFacet.setType(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL);
        hierarchyFacet.setFacetLimit(7);
        hierarchyFacet.setSortOrderSidebar(DiscoveryConfigurationParameters.SORT.VALUE);
        discoveryConfiguration.setSidebarFacets(Arrays.asList(subjectFacet, dateFacet, hierarchyFacet));
        discoveryConfiguration.setSearchFilters(Arrays.asList(subjectFacet, dateFacet, hierarchyFacet));

        query = "my test case";
        searchFilter = new SearchFilter("subject", "equals", "Java");
        page = new PageRequest(1, 10, Sort.Direction.ASC, "dc.title");

        queryBuilder.afterPropertiesSet();
    }

    @Test
    public void testBuildQuery() throws Exception {

        DiscoverQuery discoverQuery = queryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "item", page);

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true", "subject:\"Java\""));
        assertThat(discoverQuery.getQuery(), is(query));
        assertThat(discoverQuery.getDSpaceObjectFilter(), is(Constants.ITEM));
        assertThat(discoverQuery.getSortField(), is("dc.title_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.asc));
        assertThat(discoverQuery.getMaxResults(), is(10));
        assertThat(discoverQuery.getStart(), is(10));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverFacetField("subject", DiscoveryConfigurationParameters.TYPE_TEXT, 6, DiscoveryConfigurationParameters.SORT.COUNT)),
                new ReflectionEquals(new DiscoverFacetField("hierarchy", DiscoveryConfigurationParameters.TYPE_HIERARCHICAL, 8, DiscoveryConfigurationParameters.SORT.VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                new ReflectionEquals(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test
    public void testBuildQueryDefaults() throws Exception {
        DiscoverQuery discoverQuery = queryBuilder.buildQuery(context, null, discoveryConfiguration, null,
                null, null, null);

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true"));
        assertThat(discoverQuery.getQuery(), isEmptyOrNullString());
        assertThat(discoverQuery.getDSpaceObjectFilter(), is(-1));
        //Note this should actually be "dc.date.accessioned_dt"  but remember that our searchService is just a stupid mock
        assertThat(discoverQuery.getSortField(), is("dc.date.accessioned_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.desc));
        assertThat(discoverQuery.getMaxResults(), is(100));
        assertThat(discoverQuery.getStart(), is(0));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverFacetField("subject", DiscoveryConfigurationParameters.TYPE_TEXT, 6, DiscoveryConfigurationParameters.SORT.COUNT)),
                new ReflectionEquals(new DiscoverFacetField("hierarchy", DiscoveryConfigurationParameters.TYPE_HIERARCHICAL, 8, DiscoveryConfigurationParameters.SORT.VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                new ReflectionEquals(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test
    public void testSortByScore() throws Exception {
        page = new PageRequest(2, 10, Sort.Direction.ASC, "SCORE");

        DiscoverQuery discoverQuery = queryBuilder.buildQuery(context, null, discoveryConfiguration, null,
                null, null, page);

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true"));
        assertThat(discoverQuery.getQuery(), isEmptyOrNullString());
        assertThat(discoverQuery.getDSpaceObjectFilter(), is(-1));
        //Note this should actually be "dc.date.accessioned_dt"  but remember that our searchService is just a stupid mock
        assertThat(discoverQuery.getSortField(), is("score_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.asc));
        assertThat(discoverQuery.getMaxResults(), is(10));
        assertThat(discoverQuery.getStart(), is(20));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverFacetField("subject", DiscoveryConfigurationParameters.TYPE_TEXT, 6, DiscoveryConfigurationParameters.SORT.COUNT)),
                new ReflectionEquals(new DiscoverFacetField("hierarchy", DiscoveryConfigurationParameters.TYPE_HIERARCHICAL, 8, DiscoveryConfigurationParameters.SORT.VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                new ReflectionEquals(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                new ReflectionEquals(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test(expected = InvalidDSpaceObjectTypeException.class)
    public void testInvalidDSOType() throws Exception {
        queryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "TEST", page);
    }

    @Test(expected = InvalidSortingException.class)
    public void testInvalidSortField() throws Exception {
        page = new PageRequest(2, 10, Sort.Direction.ASC, "test");
        queryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "ITEM", page);
    }

    @Test(expected = InvalidSearchFilterException.class)
    public void testInvalidSearchFilter1() throws Exception {
        searchFilter = new SearchFilter("test", "equals", "Smith, Donald");

        queryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "ITEM", page);
    }

    @Test(expected = InvalidSearchFilterException.class)
    public void testInvalidSearchFilter2() throws Exception {
        when(searchService.toFilterQuery(any(Context.class), any(String.class), any(String.class), any(String.class)))
                .thenThrow(SQLException.class);

        queryBuilder.buildQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "ITEM", page);
    }

    @Test
    public void testBuildFacetQuery() throws Exception {
        DiscoverQuery discoverQuery = queryBuilder.buildFacetQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "item", page, "subject");

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true", "subject:\"Java\""));
        assertThat(discoverQuery.getQuery(), is(query));
        assertThat(discoverQuery.getDSpaceObjectFilter(), is(Constants.ITEM));
        assertThat(discoverQuery.getSortField(), isEmptyOrNullString());
        assertThat(discoverQuery.getMaxResults(), is(0));
        assertThat(discoverQuery.getStart(), is(0));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(10));
        assertThat(discoverQuery.getFacetFields(), hasSize(1));
        assertThat(discoverQuery.getFacetFields(), contains(
                new ReflectionEquals(new DiscoverFacetField("subject", DiscoveryConfigurationParameters.TYPE_TEXT, 11, DiscoveryConfigurationParameters.SORT.COUNT))
        ));
    }

    @Test(expected = InvalidSearchFacetException.class)
    public void testInvalidSearchFacet() throws Exception {
        queryBuilder.buildFacetQuery(context, scope, discoveryConfiguration, query,
                Arrays.asList(searchFilter), "item", page, "test");
    }

}