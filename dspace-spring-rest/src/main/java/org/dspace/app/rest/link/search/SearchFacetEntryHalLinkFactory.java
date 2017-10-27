/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.model.DiscoveryResultsRest;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.hateoas.SearchFacetEntryResource;
import org.dspace.app.rest.model.hateoas.SearchFacetValueResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This factory provides a means to add links to the SearchFacetEntryResource. This class and addLinks method will be called
 * from the HalLinkService addLinks method is called if the HalResource given is eligible
 */
@Component
public class SearchFacetEntryHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchFacetEntryResource> {
    @Override
    protected void addLinks(SearchFacetEntryResource halResource, Pageable pageable, LinkedList<Link> list) {
        SearchFacetEntryRest facetData = halResource.getFacetData();
        DiscoveryResultsRest searchData = halResource.getSearchData();

        UriComponentsBuilder uriBuilder = uriBuilder(getMethodOn()
                .getFacetValues(facetData.getName(), searchData.getQuery(), searchData.getDsoType(), searchData.getScope(), null, null));

        addFilterParams(uriBuilder, searchData);

        list.add(buildLink(Link.REL_SELF, uriBuilder.build().toUriString()));
    }

    @Override
    protected Class<SearchFacetEntryResource> getResourceClass() {
        return SearchFacetEntryResource.class;
    }

}
