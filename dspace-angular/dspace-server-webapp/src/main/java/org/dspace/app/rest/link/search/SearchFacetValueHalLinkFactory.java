/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.hateoas.SearchFacetValueResource;
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
public class SearchFacetValueHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchFacetValueResource> {

    @Override
    protected void addLinks(SearchFacetValueResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        if (halResource.getSearchData() != null && halResource.getFacetData() != null && halResource
            .getValueData() != null) {

            UriComponentsBuilder builder = buildSearchBaseLink(halResource.getSearchData());

            addFilterForFacetValue(builder, halResource.getFacetData(), halResource.getValueData());

            list.add(buildLink("search", builder.build().toUriString()));

        }
    }

    @Override
    protected Class<SearchFacetValueResource> getResourceClass() {
        return SearchFacetValueResource.class;
    }

    private void addFilterForFacetValue(final UriComponentsBuilder baseLink,
                                        SearchFacetEntryRest facetData, SearchFacetValueRest valueData) {
        baseLink.queryParam("f." + facetData.getName(), valueData.getFilterValue() + "," + valueData.getFilterType());
    }
}
