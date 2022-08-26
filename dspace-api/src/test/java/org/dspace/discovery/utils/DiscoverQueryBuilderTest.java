/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.utils;

import static java.util.Collections.emptyList;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT.COUNT;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.SORT.VALUE;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.TYPE_HIERARCHICAL;
import static org.dspace.discovery.configuration.DiscoveryConfigurationParameters.TYPE_TEXT;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.dspace.core.Context;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.discovery.configuration.HierarchicalSidebarFacetConfiguration;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;
import org.dspace.services.ConfigurationService;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


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
    private IndexableObject scope;

    @Mock
    private IndexFactory indexFactory;

    private DiscoveryConfiguration discoveryConfiguration;
    private String query;

    private int pageSize = 10;
    private long offset = 10;
    private String sortProperty = "dc.title";
    private String sortDirection = "ASC";

    private QueryBuilderSearchFilter searchFilter;


    @Before
    public void setUp() throws Exception {
        queryBuilder.setIndexableFactories(Collections.singletonList(indexFactory));

        when(indexFactory.getType()).thenReturn(IndexableItem.TYPE);

        when(configurationService.getIntProperty(eq("rest.search.max.results"), anyInt())).thenReturn(100);

        when(searchService.toSortFieldIndex(any(String.class), any(String.class)))
                .then(invocation -> invocation.getArguments()[0] + "_sort");

        when(searchService
                     .getFacetYearRange(eq(context), nullable(IndexableObject.class),
                                        any(DiscoverySearchFilterFacet.class),
                                        any(), any(DiscoverQuery.class)))
                .then(invocation -> new FacetYearRange((DiscoverySearchFilterFacet) invocation.getArguments()[2]));

        when(searchService.toFilterQuery(any(Context.class), any(String.class), any(String.class), any(String.class),
                                         any(DiscoveryConfiguration.class)))
                .then(invocation -> new DiscoverFilterQuery((String) invocation.getArguments()[1],
                                                            invocation.getArguments()[1] + ":\"" + invocation
                                                                    .getArguments()[3] + "\"",
                                                            (String) invocation.getArguments()[3]));

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

        searchFilter = new QueryBuilderSearchFilter("subject", "equals", "Java");
        query = "my test case";

        queryBuilder.afterPropertiesSet();
    }

    @Test
    public void testBuildQuery() throws Exception {

        DiscoverQuery discoverQuery = queryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Collections.singletonList(searchFilter),
                            "item", pageSize, offset, sortProperty, sortDirection);

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true", "subject:\"Java\""));
        assertThat(discoverQuery.getQuery(), is(query));
        assertThat(discoverQuery.getDSpaceObjectFilters(), contains(IndexableItem.TYPE));
        assertThat(discoverQuery.getSortField(), is("dc.title_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.asc));
        assertThat(discoverQuery.getMaxResults(), is(10));
        assertThat(discoverQuery.getStart(), is(10));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                discoverFacetFieldMatcher(new DiscoverFacetField("subject", TYPE_TEXT, 6, COUNT)),
                discoverFacetFieldMatcher(new DiscoverFacetField("hierarchy", TYPE_HIERARCHICAL, 8, VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test
    public void testBuildQueryDefaults() throws Exception {
        DiscoverQuery discoverQuery =
                queryBuilder.buildQuery(context, null, discoveryConfiguration, null, null, emptyList(), null, null,
                                        null, null);

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true"));
        assertThat(discoverQuery.getQuery(), isEmptyOrNullString());
        assertThat(discoverQuery.getDSpaceObjectFilters(), is(empty()));
        //Note this should actually be "dc.date.accessioned_dt"  but remember that our searchService is just a stupid
        // mock
        assertThat(discoverQuery.getSortField(), is("dc.date.accessioned_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.desc));
        assertThat(discoverQuery.getMaxResults(), is(100));
        assertThat(discoverQuery.getStart(), is(0));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                discoverFacetFieldMatcher(new DiscoverFacetField("subject", TYPE_TEXT, 6, COUNT)),
                discoverFacetFieldMatcher(new DiscoverFacetField("hierarchy", TYPE_HIERARCHICAL, 8, VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test
    public void testSortByScore() throws Exception {
        DiscoverQuery discoverQuery =
                queryBuilder.buildQuery(context, null, discoveryConfiguration, null, null, emptyList(), 10, 20L,
                                        "SCORE", "ASC");

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true"));
        assertThat(discoverQuery.getQuery(), isEmptyOrNullString());
        assertThat(discoverQuery.getDSpaceObjectFilters(), is(empty()));
        //Note this should actually be "dc.date.accessioned_dt"  but remember that our searchService is just a stupid
        // mock
        assertThat(discoverQuery.getSortField(), is("score_sort"));
        assertThat(discoverQuery.getSortOrder(), is(DiscoverQuery.SORT_ORDER.asc));
        assertThat(discoverQuery.getMaxResults(), is(10));
        assertThat(discoverQuery.getStart(), is(20));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(0));
        assertThat(discoverQuery.getFacetFields(), hasSize(2));
        assertThat(discoverQuery.getFacetFields(), containsInAnyOrder(
                discoverFacetFieldMatcher(new DiscoverFacetField("subject", TYPE_TEXT, 6, COUNT)),
                discoverFacetFieldMatcher(new DiscoverFacetField("hierarchy", TYPE_HIERARCHICAL, 8, VALUE))
        ));
        assertThat(discoverQuery.getHitHighlightingFields(), hasSize(2));
        assertThat(discoverQuery.getHitHighlightingFields(), containsInAnyOrder(
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("dc.title", 0, 3)),
                discoverHitHighlightingFieldMatcher(new DiscoverHitHighlightingField("fulltext", 0, 3))
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDSOType() throws Exception {
        queryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Collections.singletonList(searchFilter),
                            "TEST", pageSize, offset, sortProperty, sortDirection);
    }

    @Test(expected = SearchServiceException.class)
    public void testInvalidSortField() throws Exception {
        queryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Collections.singletonList(searchFilter),
                            "ITEM", pageSize, 20L, "test", sortDirection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSearchFilter1() throws Exception {
        searchFilter = new QueryBuilderSearchFilter("test", "equals", "Smith, Donald");

        queryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Arrays.asList(searchFilter), "ITEM",
                            pageSize, offset, sortProperty, sortDirection);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSearchFilter2() throws Exception {
        when(searchService.toFilterQuery(any(Context.class), any(String.class), any(String.class), any(String.class),
                                         any(DiscoveryConfiguration.class)))
                .thenThrow(SQLException.class);

        queryBuilder
                .buildQuery(context, scope, discoveryConfiguration, query, Arrays.asList(searchFilter), "ITEM",
                            pageSize, offset, sortProperty, sortDirection);
    }

    @Test
    public void testBuildFacetQuery() throws Exception {
        DiscoverQuery discoverQuery = queryBuilder.buildFacetQuery(context, scope, discoveryConfiguration, "prefix",
                                                                   query, Collections.singletonList(searchFilter),
                                                                   "item", pageSize, offset, "subject");

        assertThat(discoverQuery.getFilterQueries(), containsInAnyOrder("archived:true", "subject:\"Java\""));
        assertThat(discoverQuery.getQuery(), is(query));
        assertThat(discoverQuery.getDSpaceObjectFilters(), contains(IndexableItem.TYPE));
        assertThat(discoverQuery.getSortField(), isEmptyOrNullString());
        assertThat(discoverQuery.getMaxResults(), is(0));
        assertThat(discoverQuery.getStart(), is(0));
        assertThat(discoverQuery.getFacetMinCount(), is(1));
        assertThat(discoverQuery.getFacetOffset(), is(10));
        assertThat(discoverQuery.getFacetFields(), hasSize(1));
        assertThat(discoverQuery.getFacetFields(), contains(
                discoverFacetFieldMatcher(new DiscoverFacetField("subject", TYPE_TEXT, 11, COUNT, "prefix"))
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSearchFacet() throws Exception {
        queryBuilder.buildFacetQuery(context, scope, discoveryConfiguration, null, query,
                                     Collections.singletonList(searchFilter), "item", pageSize, offset, "test");
    }

    public Matcher<DiscoverFacetField> discoverFacetFieldMatcher(DiscoverFacetField expected) {

        return allOf(
                hasProperty(expected, DiscoverFacetField::getField, "field"),
                hasProperty(expected, DiscoverFacetField::getType, "type"),
                hasProperty(expected, DiscoverFacetField::getPrefix, "prefix"),
                hasProperty(expected, DiscoverFacetField::getLimit, "limit"),
                hasProperty(expected, DiscoverFacetField::getOffset, "offset")
        );
    }

    public Matcher<DiscoverHitHighlightingField> discoverHitHighlightingFieldMatcher
            (DiscoverHitHighlightingField expected) {

        return allOf(
                hasProperty(expected, DiscoverHitHighlightingField::getField, "field"),
                hasProperty(expected, DiscoverHitHighlightingField::getMaxChars, "maxChars"),
                hasProperty(expected, DiscoverHitHighlightingField::getMaxSnippets, "maxSnippets")
        );
    }

    public <T, U> FeatureMatcher<T, U> hasProperty(T expected, Function<T, U> property, String description) {

        return new FeatureMatcher<T, U>
                (equalTo(property.apply(expected)), description, description) {

            protected U featureValueOf(T actual) {
                return property.apply(actual);
            }
        };
    }
}
