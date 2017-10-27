/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
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

    protected UriComponentsBuilder buildSearchBaseLink(final DiscoveryResultsRest data) {
        UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                .getSearchObjects(data.getQuery(), data.getDsoType(), data.getScope(), data.getConfigurationName(), null, null));

        return addFilterParams(uriBuilder, data);
    }

    protected UriComponentsBuilder buildFacetBaseLink(final FacetResultsRest data) {
        UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                .getFacetValues(data.getFacetEntry().getName(), data.getQuery(), data.getDsoType(), data.getScope(), null, null));

        return addFilterParams(uriBuilder, data);
    }

    protected UriComponentsBuilder addFilterParams(UriComponentsBuilder uriComponentsBuilder, DiscoveryResultsRest data) {
        if (data.getAppliedFilters() != null) {
            for (SearchResultsRest.AppliedFilter filter : data.getAppliedFilters()) {
                //TODO Make sure the filter format is defined in only one place
                uriComponentsBuilder.queryParam("f." + filter.getFilter(), filter.getValue() + "," + filter.getOperator());
            }
        }

        return uriComponentsBuilder;
    }

    @Override
    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
