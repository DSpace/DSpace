/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.SearchFacetEntryRest;
import org.dspace.app.rest.model.hateoas.SearchFacetEntryResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This factory provides a means to add links to the SearchFacetEntryResource. This class and addLinks method will be
 * called
 * from the HalLinkService addLinks method is called if the HalResource given is eligible
 */
@Component
public class SearchFacetEntryHalLinkFactory extends HalLinkFactory<SearchFacetEntryResource, RestResourceController> {
    @Override
    protected void addLinks(SearchFacetEntryResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        SearchFacetEntryRest facet = halResource.getContent();
        if (facet != null && facet.getFacetInformation() != null) {
            List<Link> updatedLinks = new ArrayList<>();
            halResource.getLinks().forEach(link -> {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(link.getHref());
                facet.getFacetInformation().appendQueryParameters(builder, true);
                updatedLinks.add(buildLink(link.getRel().value(), builder.build().encode().toUriString()));
            });
            halResource.removeLinks();
            list.addAll(updatedLinks);
        }
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<SearchFacetEntryResource> getResourceClass() {
        return SearchFacetEntryResource.class;
    }
}
