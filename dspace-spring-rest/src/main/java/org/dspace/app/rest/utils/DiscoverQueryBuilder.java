/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.exception.InvalidDSpaceObjectTypeException;
import org.dspace.app.rest.exception.InvalidRequestException;
import org.dspace.app.rest.exception.InvalidSearchFilterException;
import org.dspace.app.rest.exception.InvalidSortingException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.*;
import org.dspace.discovery.configuration.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tom on 19/09/2017.
 * TODO TOM UNIT TEST
 */
@Component
public class DiscoverQueryBuilder {

    private static final Logger log = Logger.getLogger(DiscoverQueryBuilder.class);

    @Autowired
    private SearchService searchService;

    public DiscoverQuery buildQuery(Context context, DSpaceObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    String dsoType, Pageable page)
            throws InvalidRequestException {

        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration);

        //Add search filters
        queryArgs.addFilterQueries(convertFilters(context, searchFilters));

        //Set search query
        if (StringUtils.isNotBlank(query)) {
            queryArgs.setQuery(searchService.escapeQueryChars(query));
        }

        //Limit results to DSO type
        if (StringUtils.isNotBlank(dsoType)) {
            queryArgs.setDSpaceObjectFilter(getDsoTypeId(dsoType));
        }

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting
        configurePagination(page, queryArgs);
        configureSorting(page, queryArgs, discoveryConfiguration.getSearchSortConfiguration());

        return queryArgs;
    }

    private DiscoverQuery buildBaseQueryForConfiguration(DiscoveryConfiguration discoveryConfiguration) {
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries()
                .toArray(new String[discoveryConfiguration.getDefaultFilterQueries().size()]));
        return queryArgs;
    }

    private void configureSorting(Pageable page, DiscoverQuery queryArgs, DiscoverySortConfiguration searchSortConfiguration) throws InvalidSortingException {
        String sortBy = null;
        String sortOrder = null;

        //Read the Pageable object if there is one
        if (page != null) {
            Sort sort = page.getSort();
            if (sort != null && sort.iterator().hasNext()) {
                Sort.Order order = sort.iterator().next();
                sortBy = order.getProperty();
                sortOrder = order.getDirection().name();
            }
        }

        //Load defaults if we did not receive values
        if (sortBy == null) {
            sortBy = getDefaultSortField(searchSortConfiguration);
        }
        if (sortOrder == null) {
            sortOrder = getDefaultSortDirection(searchSortConfiguration, sortOrder);
        }

        //Update Discovery query
        if (sortBy != null && searchSortConfiguration.isValidSortField(sortBy)) {
            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortBy, DiscoverQuery.SORT_ORDER.desc);
            } else {
                throw new InvalidSortingException(sortOrder + " is not a valid sort order");
            }
        } else {
            throw new InvalidSortingException(sortBy + " is not a valid sort field");
        }
    }

    private String getDefaultSortDirection(DiscoverySortConfiguration searchSortConfiguration, String sortOrder) {
        if (searchSortConfiguration != null) {
            sortOrder = searchSortConfiguration.getDefaultSortOrder()
                    .toString();
        }
        return sortOrder;
    }

    private String getDefaultSortField(DiscoverySortConfiguration searchSortConfiguration) {
        String sortBy;// Attempt to find the default one, if none found we use SCORE
        sortBy = "score";
        if (searchSortConfiguration != null) {
            for (DiscoverySortFieldConfiguration sortFieldConfiguration : searchSortConfiguration
                    .getSortFields()) {
                if (sortFieldConfiguration.equals(searchSortConfiguration
                        .getDefaultSort())) {
                    sortBy = SearchUtils
                            .getSearchService()
                            .toSortFieldIndex(
                                    sortFieldConfiguration
                                            .getMetadataField(),
                                    sortFieldConfiguration.getType());
                }
            }
        }
        return sortBy;
    }

    private void configurePagination(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setMaxResults(page.getPageSize());
            queryArgs.setStart(page.getOffset());
        }
    }

    private int getDsoTypeId(String dsoType) throws InvalidDSpaceObjectTypeException {
        int index = ArrayUtils.indexOf(Constants.typeText, dsoType.toUpperCase());
        if (index < 0) {
            throw new InvalidDSpaceObjectTypeException(dsoType + " is not a valid DSpace Object type");
        }
        return index;
    }

    private String[] convertFilters(Context context, List<SearchFilter> searchFilters) throws InvalidSearchFilterException {
        ArrayList<String> filterQueries = new ArrayList<>(searchFilters.size());

        try {
            //TODO TOM take into account OR filters
            for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(searchFilters)) {
                DiscoverFilterQuery filterQuery = searchService.toFilterQuery(context,
                        searchFilter.getName(), searchFilter.getOperator(), searchFilter.getValue());

                if (filterQuery != null) {
                    filterQueries.add(filterQuery.getFilterQuery());
                }

            }
        } catch (SQLException e) {
            throw new InvalidSearchFilterException("There was a problem parsing the search filters.", e);
        }

        return filterQueries.toArray(new String[filterQueries.size()]);
    }

    private DiscoverQuery addFaceting(Context context, DSpaceObject scope, DiscoverQuery queryArgs, DiscoveryConfiguration discoveryConfiguration) {

        List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        log.debug("facets for configuration " + discoveryConfiguration.getId() + ": " + (facets != null ? facets.size() : null));

        if (facets != null) {
            queryArgs.setFacetMinCount(1);

            /** enable faceting of search results */
            for (DiscoverySearchFilterFacet facet : facets) {
                if (facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
                    try {
                        FacetYearRange facetYearRange = searchService.getFacetYearRange(context, scope, facet, queryArgs.getFilterQueries());

                        queryArgs.addYearRangeFacet(facet, facetYearRange);

                    } catch (Exception e) {
                        log.error(LogManager.getHeader(context, "Error in Discovery while setting up date facet range", "date facet: " + facet), e);
                    }

                } else {

                    int facetLimit = facet.getFacetLimit();
                    //Add one to our facet limit to make sure that if we have more then the shown facets that we show our "show more" url
                    facetLimit++;

                    queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit, facet.getSortOrderSidebar()));
                }
            }
        }

        return queryArgs;
    }

}
