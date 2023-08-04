/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT.VALUE;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.TYPE_HIERARCHICAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.InvalidSearchRequestException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for {@link RestDiscoverQueryBuilder}
 */
@RunWith(MockitoJUnitRunner.class)
public class RestDiscoverQueryBuilderTest {

    @InjectMocks
    private RestDiscoverQueryBuilder restQueryBuilder;

    @Mock
    private DiscoverQueryBuilder discoverQueryBuilder;

    @Mock
    private Context context;

    @Mock
    private IndexableObject scope;

    private DiscoveryConfiguration discoveryConfiguration;
    private String query;
    private SearchFilter searchFilter;
    private QueryBuilderSearchFilter tranformedFilter;

    private PageRequest page;

    @Before
    public void setUp() throws Exception {
        discoveryConfiguration = new DiscoveryConfiguration();
        discoveryConfiguration.setDefaultFilterQueries(Arrays.asList("archived:true"));


        DiscoveryHitHighlightingConfiguration discoveryHitHighlightingConfiguration =
                new DiscoveryHitHighlightingConfiguration();
        List<DiscoveryHitHighlightFieldConfiguration> discoveryHitHighlightFieldConfigurations = new LinkedList<>();

        DiscoveryHitHighlightFieldConfiguration discoveryHitHighlightFieldConfiguration =
                new DiscoveryHitHighlightFieldConfiguration();
        discoveryHitHighlightFieldConfiguration.setField("dc.title");

        DiscoveryHitHighlightFieldConfiguration discoveryHitHighlightFieldConfiguration1 =
                new DiscoveryHitHighlightFieldConfiguration();
        discoveryHitHighlightFieldConfiguration1.setField("fulltext");

        discoveryHitHighlightFieldConfigurations.add(discoveryHitHighlightFieldConfiguration1);
        discoveryHitHighlightFieldConfigurations.add(discoveryHitHighlightFieldConfiguration);

        discoveryHitHighlightingConfiguration.setMetadataFields(discoveryHitHighlightFieldConfigurations);
        discoveryConfiguration.setHitHighlightingConfiguration(discoveryHitHighlightingConfiguration);


        DiscoverySortFieldConfiguration defaultSort = new DiscoverySortFieldConfiguration();
        defaultSort.setMetadataField("dc.date.accessioned");
        defaultSort.setType(DiscoveryConfigurationParameters.TYPE_DATE);
        defaultSort.setDefaultSortOrder(DiscoverySortFieldConfiguration.SORT_ORDER.desc);


        List<DiscoverySortFieldConfiguration> listSortField = new ArrayList<DiscoverySortFieldConfiguration>();
        listSortField.add(defaultSort);

        DiscoverySortConfiguration sortConfiguration = new DiscoverySortConfiguration();

        DiscoverySortFieldConfiguration titleSort = new DiscoverySortFieldConfiguration();
        titleSort.setMetadataField("dc.title");
        titleSort.setDefaultSortOrder(DiscoverySortFieldConfiguration.SORT_ORDER.asc);
        listSortField.add(titleSort);

        sortConfiguration.setSortFields(listSortField);

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
        hierarchyFacet.setType(TYPE_HIERARCHICAL);
        hierarchyFacet.setFacetLimit(7);
        hierarchyFacet.setSortOrderSidebar(VALUE);
        discoveryConfiguration.setSidebarFacets(Arrays.asList(subjectFacet, dateFacet, hierarchyFacet));
        discoveryConfiguration.setSearchFilters(Arrays.asList(subjectFacet, dateFacet, hierarchyFacet));

        query = "my test case";
        searchFilter = new SearchFilter("subject", "equals", "Java");
        tranformedFilter = new QueryBuilderSearchFilter("subject", "equals", "Java");
        page = PageRequest.of(1, 10, Sort.Direction.ASC, "dc.title");
    }

    @Test
    public void testBuildQuery() throws Exception {
        restQueryBuilder.buildQuery(context, scope, discoveryConfiguration, query, Arrays.asList(searchFilter), "item",
                                    page);

        verify(discoverQueryBuilder, times(1)).buildQuery(context, scope, discoveryConfiguration, query,
                                                          Arrays.asList(tranformedFilter), singletonList("item"),
                                                          page.getPageSize(), page.getOffset(), "dc.title",
                                                          "ASC");
    }

    @Test
    public void testBuildQueryDefaults() throws Exception {
        restQueryBuilder.buildQuery(context, null, discoveryConfiguration, null, null, emptyList(), null);

        verify(discoverQueryBuilder, times(1)).buildQuery(context, null, discoveryConfiguration, null,
                                                          emptyList(), emptyList(), null, null, null, null);
    }

    @Test
    public void testSortByScore() throws Exception {
        page = PageRequest.of(2, 10, Sort.Direction.ASC, "SCORE");
        restQueryBuilder.buildQuery(context, null, discoveryConfiguration, null, null, emptyList(), page);

        verify(discoverQueryBuilder, times(1)).buildQuery(context, null, discoveryConfiguration, null,
                                                          emptyList(), emptyList(), page.getPageSize(),
                                                          page.getOffset(), "SCORE", "ASC");
    }

    @Test(expected = DSpaceBadRequestException.class)
    public void testCatchIllegalArgumentException() throws Exception {
        when(discoverQueryBuilder.buildQuery(any(), any(), any(), any(), any(), anyList(), any(), any(), any(),
                                             any())).thenThrow(IllegalArgumentException.class);
        restQueryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Arrays.asList(searchFilter), "TEST", page);
    }

    @Test(expected = InvalidSearchRequestException.class)
    public void testCatchSearchServiceException() throws Exception {
        when(discoverQueryBuilder.buildQuery(any(), any(), any(), any(), any(), anyList(), any(), any(), any(),
                                             any())).thenThrow(SearchServiceException.class);
        restQueryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Arrays.asList(searchFilter), "ITEM", page);
    }

    @Test
    public void testBuildFacetQuery() throws Exception {
        restQueryBuilder.buildFacetQuery(context, scope, discoveryConfiguration,
                                         "prefix", query,
                                         singletonList(searchFilter), "item", page,
                                         "subject");

        verify(discoverQueryBuilder, times(1)).buildFacetQuery(context, scope, discoveryConfiguration, "prefix",
                                                               query, singletonList(tranformedFilter),
                                                               singletonList("item"), page.getPageSize(),
                                                               page.getOffset(), "subject");
    }
}
