/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.rest.converter.query.SearchQueryConverter;
import org.dspace.app.rest.exception.InvalidDSpaceObjectTypeException;
import org.dspace.app.rest.exception.InvalidRequestException;
import org.dspace.app.rest.exception.InvalidSearchFacetException;
import org.dspace.app.rest.exception.InvalidSearchFilterException;
import org.dspace.app.rest.exception.InvalidSortingException;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverFacetField;
import org.dspace.discovery.DiscoverFilterQuery;
import org.dspace.discovery.DiscoverHitHighlightingField;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.FacetYearRange;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;
import org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration;
import org.dspace.discovery.configuration.DiscoverySearchFilter;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.discovery.configuration.DiscoverySortConfiguration;
import org.dspace.discovery.configuration.DiscoverySortFieldConfiguration;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * This class builds the queries for the /search and /facet endpoints.
 */
@Component
public class DiscoverQueryBuilder implements InitializingBean {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DiscoverQueryBuilder.class);

    @Autowired
    private SearchService searchService;

    @Autowired
    private ConfigurationService configurationService;

    private int pageSizeLimit;

    @Override
    public void afterPropertiesSet() throws Exception {
        pageSizeLimit = configurationService.getIntProperty("rest.search.max.results", 100);
    }

    public DiscoverQuery buildQuery(Context context, DSpaceObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<SearchFilter> searchFilters,
                                    String dsoType, Pageable page)
        throws InvalidRequestException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                                                           dsoType);

        //When all search criteria are set, configure facet results
        addFaceting(context, scope, queryArgs, discoveryConfiguration);

        //Configure pagination and sorting
        configurePagination(page, queryArgs);
        configureSorting(page, queryArgs, discoveryConfiguration.getSearchSortConfiguration());

        addDiscoveryHitHighlightFields(discoveryConfiguration, queryArgs);
        return queryArgs;
    }

    private void addDiscoveryHitHighlightFields(DiscoveryConfiguration discoveryConfiguration,
                                                DiscoverQuery queryArgs) {
        if (discoveryConfiguration.getHitHighlightingConfiguration() != null) {
            List<DiscoveryHitHighlightFieldConfiguration> metadataFields = discoveryConfiguration
                .getHitHighlightingConfiguration().getMetadataFields();
            for (DiscoveryHitHighlightFieldConfiguration fieldConfiguration : metadataFields) {
                queryArgs.addHitHighlightingField(
                    new DiscoverHitHighlightingField(fieldConfiguration.getField(), fieldConfiguration.getMaxSize(),
                                                     fieldConfiguration.getSnippets()));
            }
        }
    }

    public DiscoverQuery buildFacetQuery(Context context, DSpaceObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<SearchFilter> searchFilters,
                                         String dsoType, Pageable page, String facetName)
        throws InvalidRequestException {

        DiscoverQuery queryArgs = buildCommonDiscoverQuery(context, discoveryConfiguration, query, searchFilters,
                                                           dsoType);

        //When all search criteria are set, configure facet results
        addFacetingForFacets(context, scope, prefix, queryArgs, discoveryConfiguration, facetName, page);

        //We don' want any search results, we only want facet values
        queryArgs.setMaxResults(0);

        //Configure pagination
        configurePaginationForFacets(page, queryArgs);

        return queryArgs;
    }

    private void configurePaginationForFacets(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setFacetOffset(page.getOffset());
        }
    }

    private DiscoverQuery addFacetingForFacets(Context context, DSpaceObject scope, String prefix,
                                               DiscoverQuery queryArgs, DiscoveryConfiguration discoveryConfiguration,
                                               String facetName, Pageable page) throws InvalidSearchFacetException {

        DiscoverySearchFilterFacet facet = discoveryConfiguration.getSidebarFacet(facetName);
        if (facet != null) {
            queryArgs.setFacetMinCount(1);
            int pageSize = Math.min(pageSizeLimit, page.getPageSize());

            fillFacetIntoQueryArgs(context, scope, prefix, queryArgs, facet, pageSize);

        } else {
            throw new InvalidSearchFacetException(facetName + " is not a valid search facet");
        }

        return queryArgs;
    }

    private void fillFacetIntoQueryArgs(Context context, DSpaceObject scope, String prefix, DiscoverQuery queryArgs,
                                        DiscoverySearchFilterFacet facet, final int pageSize) {
        if (facet.getType().equals(DiscoveryConfigurationParameters.TYPE_DATE)) {
            try {
                FacetYearRange facetYearRange = searchService
                    .getFacetYearRange(context, scope, facet, queryArgs.getFilterQueries());

                queryArgs.addYearRangeFacet(facet, facetYearRange);

            } catch (Exception e) {
                log.error(LogManager.getHeader(context, "Error in Discovery while setting up date facet range",
                                               "date facet: " + facet), e);
            }

        } else {

            //Add one to our facet limit to make sure that if we have more then the shown facets that we show our
            // "show more" url
            int facetLimit = pageSize + 1;
            //This should take care of the sorting for us
            queryArgs.addFacetField(new DiscoverFacetField(facet.getIndexFieldName(), facet.getType(), facetLimit,
                    facet.getSortOrderSidebar(), StringUtils.trimToNull(prefix)));
        }
    }

    private DiscoverQuery buildCommonDiscoverQuery(Context context, DiscoveryConfiguration discoveryConfiguration,
                                                   String query,
                                                   List<SearchFilter> searchFilters, String dsoType)
        throws InvalidSearchFilterException, InvalidDSpaceObjectTypeException {
        DiscoverQuery queryArgs = buildBaseQueryForConfiguration(discoveryConfiguration);

        //Add search filters
        queryArgs.addFilterQueries(convertFilters(context, discoveryConfiguration, searchFilters));

        //Set search query
        if (StringUtils.isNotBlank(query)) {
            queryArgs.setQuery(escapeQueryChars(query));
        }

        //Limit results to DSO type
        if (StringUtils.isNotBlank(dsoType)) {
            queryArgs.setDSpaceObjectFilter(getDsoTypeId(dsoType));
        }
        return queryArgs;
    }

    private DiscoverQuery buildBaseQueryForConfiguration(DiscoveryConfiguration discoveryConfiguration) {
        DiscoverQuery queryArgs = new DiscoverQuery();
        queryArgs.addFilterQueries(discoveryConfiguration.getDefaultFilterQueries()
                                                         .toArray(
                                                             new String[discoveryConfiguration.getDefaultFilterQueries()
                                                                                              .size()]));
        return queryArgs;
    }

    private void configureSorting(Pageable page, DiscoverQuery queryArgs,
                                  DiscoverySortConfiguration searchSortConfiguration) throws InvalidSortingException {
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
        DiscoverySortFieldConfiguration sortFieldConfiguration = searchSortConfiguration
            .getSortFieldConfiguration(sortBy);

        if (sortFieldConfiguration != null) {
            String sortField = searchService
                .toSortFieldIndex(sortFieldConfiguration.getMetadataField(), sortFieldConfiguration.getType());

            if ("asc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.asc);
            } else if ("desc".equalsIgnoreCase(sortOrder)) {
                queryArgs.setSortField(sortField, DiscoverQuery.SORT_ORDER.desc);
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
        if (searchSortConfiguration != null && searchSortConfiguration.getDefaultSort() != null) {
            DiscoverySortFieldConfiguration defaultSort = searchSortConfiguration.getDefaultSort();
            sortBy = defaultSort.getMetadataField();
        }
        return sortBy;
    }

    private void configurePagination(Pageable page, DiscoverQuery queryArgs) {
        if (page != null) {
            queryArgs.setMaxResults(Math.min(pageSizeLimit, page.getPageSize()));
            queryArgs.setStart(page.getOffset());
        } else {
            queryArgs.setMaxResults(pageSizeLimit);
            queryArgs.setStart(0);
        }
    }

    private int getDsoTypeId(String dsoType) throws InvalidDSpaceObjectTypeException {
        int index = ArrayUtils.indexOf(Constants.typeText, dsoType.toUpperCase());
        if (index < 0) {
            throw new InvalidDSpaceObjectTypeException(dsoType + " is not a valid DSpace Object type");
        }
        return index;
    }

    private String[] convertFilters(Context context, DiscoveryConfiguration discoveryConfiguration,
                                    List<SearchFilter> searchFilters) throws InvalidSearchFilterException {
        ArrayList<String> filterQueries = new ArrayList<>(CollectionUtils.size(searchFilters));

        SearchQueryConverter searchQueryConverter = new SearchQueryConverter();
        List<SearchFilter> transformedFilters = searchQueryConverter.convert(searchFilters);
        try {
            for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(transformedFilters)) {
                DiscoverySearchFilter filter = discoveryConfiguration.getSearchFilter(searchFilter.getName());
                if (filter == null) {
                    throw new InvalidSearchFilterException(searchFilter.getName() + " is not a valid search filter");
                }

                DiscoverFilterQuery filterQuery = searchService.toFilterQuery(context,
                                                                              filter.getIndexFieldName(),
                                                                              searchFilter.getOperator(),
                                                                              searchFilter.getValue());

                if (filterQuery != null) {
                    filterQueries.add(filterQuery.getFilterQuery());
                }
            }
        } catch (SQLException e) {
            throw new InvalidSearchFilterException("There was a problem parsing the search filters.", e);
        }

        return filterQueries.toArray(new String[filterQueries.size()]);
    }

    private DiscoverQuery addFaceting(Context context, DSpaceObject scope, DiscoverQuery queryArgs,
                                      DiscoveryConfiguration discoveryConfiguration) {

        List<DiscoverySearchFilterFacet> facets = discoveryConfiguration.getSidebarFacets();

        log.debug("facets for configuration " + discoveryConfiguration.getId() + ": " + (facets != null ? facets
            .size() : null));

        if (facets != null) {
            queryArgs.setFacetMinCount(1);

            /** enable faceting of search results */
            for (DiscoverySearchFilterFacet facet : facets) {
                fillFacetIntoQueryArgs(context, scope, null, queryArgs, facet, facet.getFacetLimit());
            }
        }

        return queryArgs;
    }
    /**
     * Escape some solr special characters from the user's query.
     *
     * 1 - when a query ends with one of solr's special characters (^, \,!, +, -,:, ||, && (,),{,},[,])
     *     (a space in between or not) (e.g. "keyword3 :") the user gets
     *     an erroneous notification or the search doesn't produce results.
     *     Those characters at the end of the query should be escaped.
     *
     * 2 - escape every colon, followed by a space (e.g. "title: subtitle")
     *     in a user's query. This is intended to let end users to pass
     *     in a title containing colon-space without requiring them to escape the colon.
     *
     * @param query user-entered query string
     * @return query escaping some of solr's special characters at the end and
     *         with a colon in colon-space sequence escaped if they occur.
     */
    public String escapeQueryChars(String query) {
        query = query.trim();

        // [+\\-&|!()\\s{}\\[\\]\\^\"\\\\:]:    Śome of the solr's special characters that need to be escaped for
        //                                      regex as well as for string.
        //                                      Regex representation of \ is \\. Therefore the string
        //                                      representation of \\ is \\\\).
        //                                      \\s is in case withespaces is in between the characters.
        // + : Match or more of the preceding token
        // (?=\s+$|$):  Matches all solr's special characters at the end of a string independently of
        //              any whitespace characters
        //            - ?= is a positive lookahead. Matches a group after the main expression without
        //              including it in the result
        //            - \s: Matches any whitespace character (spaces, tabs, line breaks )
        //            - $: Matches the end of a string
        String regx = "[+\\-&|!()\\s{}\\[\\]\\^\"\\\\:]+(?=\\s+$|$)";
        Pattern pattern = Pattern.compile(regx);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            String matcherGroup = matcher.group();
            String escapedMatcherGroup = ClientUtils.escapeQueryChars(matcherGroup);

            // Do not escape brackets if they are properly opened and closed.
            if (matcherGroup.equals(")") ||
                matcherGroup.equals("]") ||
                matcherGroup.equals("}") ||
                matcherGroup.equals("\"")) {
                String closingBracket = matcher.group();
                String openingBracket = new String();

                switch (closingBracket) {
                    case "}":
                        openingBracket = "{";
                        break;
                    case ")":
                        openingBracket = "(";
                        break;
                    case "]":
                        openingBracket = "[";
                        break;
                    case "\"":
                        openingBracket = "\"";
                        break;
                    default:
                        break;
                }

                String bracketsRegex = "\\".concat(openingBracket)
                                           .concat("(.*?)\\")
                                           .concat(closingBracket);

                if (!Pattern.compile(bracketsRegex).matcher(query).find()) {
                    query = StringUtils.replace(query, matcherGroup, escapedMatcherGroup);
                }
            } else {
                query = StringUtils.replace(query, matcherGroup, escapedMatcherGroup);
            }
        }

        query = StringUtils.replace(query, ": ", "\\:");
        return query;
    }

}
