/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.query.SearchQueryConverter;
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.parameter.SearchFilter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to create a SearchResultsRest object from the given parameters
 */
@Component
public class DiscoverResultConverter {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    protected ConverterService converter;

    @Autowired
    protected Utils utils;

    @Autowired
    private SearchFilterToAppliedFilterConverter searchFilterToAppliedFilterConverter;

    @Autowired
    private PagedResourcesAssembler pageAssembler;

    public SearchResultsRest convert(final Context context, final String query, final List<String> dsoTypes,
                                     final String configurationName, final String scope,
                                     final List<SearchFilter> searchFilters, final Pageable page,
                                     final DiscoverResult searchResult, final Projection projection) {
        List<DiscoverResult.IndexableObjectHighlightResult> results = searchResult.getAllHighlightedResults();
        Page<SearchResultEntryRest> entries = converter.toRestPage(results, page, searchResult.getTotalSearchResults(),
            utils.obtainProjection());
        PagedModel<SearchResultEntryRest> pagedModel = pageAssembler
            .toModel(entries.map((entry) -> converter.toResource(entry)));

        SearchResultsRest resultsRest = new SearchResultsRest(pagedModel);
        resultsRest.setProjection(projection);

        setRequestInformation(context, query, dsoTypes, configurationName, scope, searchFilters, page, resultsRest);

        return resultsRest;
    }

    private void setRequestInformation(final Context context, final String query, final List<String> dsoTypes,
                                       final String configurationName, final String scope,
                                       final List<SearchFilter> searchFilters, final Pageable page,
                                       final SearchResultsRest resultsRest) {
        resultsRest.setQuery(query);
        resultsRest.setConfiguration(configurationName);
        resultsRest.setDsoTypes(dsoTypes);

        resultsRest.setScope(scope);

        if (page != null && page.getSort() != null && page.getSort().iterator().hasNext()) {
            Sort.Order order = page.getSort().iterator().next();
            resultsRest.setSort(order.getProperty(), order.getDirection().name());
        }
        SearchQueryConverter searchQueryConverter = new SearchQueryConverter();
        List<SearchFilter> transformedFilters = searchQueryConverter.convert(searchFilters);

        for (SearchFilter searchFilter : CollectionUtils.emptyIfNull(transformedFilters)) {
            resultsRest
                    .addAppliedFilter(searchFilterToAppliedFilterConverter.convertSearchFilter(context, searchFilter));
        }
    }
}
