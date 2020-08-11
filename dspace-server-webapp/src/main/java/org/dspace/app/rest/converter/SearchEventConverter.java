/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.PageRest;
import org.dspace.app.rest.model.SearchEventRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.utils.ScopeResolver;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SearchEventConverter {

    @Autowired
    private ScopeResolver scopeResolver;

    public UsageSearchEvent convert(Context context, HttpServletRequest request, SearchEventRest searchEventRest) {
        UsageSearchEvent usageSearchEvent = new UsageSearchEvent(UsageEvent.Action.SEARCH, request, context,
                                                                             null);
        usageSearchEvent.setQuery(searchEventRest.getQuery());
        usageSearchEvent.setDsoType(searchEventRest.getDsoType());
        IndexableObject scopeObject = scopeResolver.resolveScope(context, String.valueOf(searchEventRest.getScope()));
        if (scopeObject instanceof DSpaceObject) {
            usageSearchEvent.setScope((DSpaceObject) scopeObject);
        }
        usageSearchEvent.setConfiguration(searchEventRest.getConfiguration());
        if (searchEventRest.getAppliedFilters() != null) {
            usageSearchEvent.setAppliedFilters(convertAppliedFilters(searchEventRest.getAppliedFilters()));
        }
        usageSearchEvent.setSort(convertSort(searchEventRest.getSort()));
        usageSearchEvent.setPage(convertPage(searchEventRest.getPage()));

        return usageSearchEvent;

    }

    private UsageSearchEvent.Page convertPage(PageRest page) {
        return new UsageSearchEvent.Page(page.getSize(), page.getTotalElements(), page.getTotalPages(),
                                             page.getNumber());
    }

    private UsageSearchEvent.Sort convertSort(SearchResultsRest.Sorting sort) {
        return new UsageSearchEvent.Sort(sort.getBy(), sort.getOrder());
    }

    private List<UsageSearchEvent.AppliedFilter> convertAppliedFilters(
        List<SearchResultsRest.AppliedFilter> appliedFilters) {
        List<UsageSearchEvent.AppliedFilter> listToReturn = new LinkedList<>();
        for (SearchResultsRest.AppliedFilter appliedFilter : appliedFilters) {
            UsageSearchEvent.AppliedFilter convertedAppliedFilter = new UsageSearchEvent.AppliedFilter(
                appliedFilter.getFilter(), appliedFilter.getOperator(), appliedFilter.getValue(),
                appliedFilter.getLabel());
            listToReturn.add(convertedAppliedFilter);
        }
        return listToReturn;
    }
}
