/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.SearchFacetInformation;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.hateoas.SearchFacetValueResource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This factory provides a means to add links to the SearchFacetValueResource. This method and class will be called
 * from the
 * HalLinkService addLinks method if the given resource is eligible.
 */
@Component
public class SearchFacetValueHalLinkFactory extends HalLinkFactory<SearchFacetValueResource, RestResourceController> {
    @Autowired
    protected ConfigurationService configurationService;

    @Override
    protected void addLinks(SearchFacetValueResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        halResource.removeLinks();

        String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(dspaceServerUrl + "/api/discover/search/objects");
        addDiscoveryParameters(builder, halResource.getContent());
        list.add(buildLink("search", builder.build().encode().toUriString()));
    }

    private void addDiscoveryParameters(UriComponentsBuilder builder, SearchFacetValueRest value) {
        if (value != null && value.getFacetInformation() != null) {
            SearchFacetInformation information = value.getFacetInformation();
            information.appendQueryParameters(builder, false);
            if (information.getFacetName() != null) {
                builder.queryParam("f." + information.getFacetName(),
                    value.getFilterValue() + "," + value.getFilterType());
            }
        }
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<SearchFacetValueResource> getResourceClass() {
        return SearchFacetValueResource.class;
    }
}
