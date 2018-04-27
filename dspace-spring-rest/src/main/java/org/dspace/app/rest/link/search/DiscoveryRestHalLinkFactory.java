/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.SearchResultsRest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This factory provides a means to add links to the DiscoveryRest
 */
public abstract class DiscoveryRestHalLinkFactory<T> extends HalLinkFactory<T, DiscoveryRestController> {

    public UriComponentsBuilder buildSearchBaseLink(final DiscoveryResultsRest data) {
        try {
            UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                    .getSearchObjects(data.getQuery(), data.getDsoType(),
                            data.getScope(), data.getConfigurationName(),
                            null, null));

            return addFilterParams(uriBuilder, data);
        } catch (Exception ex) {
            //The method throwing the exception is never really executed, so this exception can never occur
            return null;
        }
    }

    protected UriComponentsBuilder buildFacetBaseLink(final FacetResultsRest data) {
        try {
            UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                    .getFacetValues(data.getFacetEntry().getName(),
                            data.getQuery(), data.getDsoType(),
                            data.getScope(), null, null));

            return addFilterParams(uriBuilder, data);
        } catch (Exception ex) {
            //The method throwing the exception is never really executed, so this exception can never occur
            return null;
        }
    }

    protected UriComponentsBuilder buildSearchFacetsBaseLink(final SearchResultsRest data) {
        try {
            UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                    .getFacets(data.getQuery(), data.getDsoType(), data.getScope(),
                            data.getConfigurationName(), null, null));

            uriBuilder = addSortingParms(uriBuilder, data);

            return addFilterParams(uriBuilder, data);
        } catch (Exception ex) {
            //The method throwing the exception is never really executed, so this exception can never occur
            return null;
        }
    }

    protected UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder,
                                                   DiscoveryResultsRest data) {
        if (uriComponentsBuilder != null && data != null && data.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : data.getAppliedFilters()) {
                //TODO Make sure the filter format is defined in only one place
                uriComponentsBuilder
                        .queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }

        return uriComponentsBuilder;
    }

    protected UriComponentsBuilder addSortingParms(UriComponentsBuilder uriComponentsBuilder,
                                                   DiscoveryResultsRest data) {
        if (uriComponentsBuilder != null && data != null && data.getSort() != null) {
            uriComponentsBuilder.queryParam("sort", data.getSort().getBy() + "," + data.getSort().getOrder());
        }

        return uriComponentsBuilder;
    }

    @Override
    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
