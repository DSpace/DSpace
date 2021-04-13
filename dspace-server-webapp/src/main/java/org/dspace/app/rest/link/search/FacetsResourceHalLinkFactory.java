/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.FacetsResource;
import org.dspace.app.rest.model.hateoas.SearchFacetEntryResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class FacetsResourceHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetsResource> {

    protected void addLinks(FacetsResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        List<SearchFacetEntryResource> data = halResource.getFacetResources();
        SearchResultsRest content = halResource.getContent();

        if (content != null && data != null && pageable != null) {
            // FIXME this is needed to avoid that the SELF link is set using the search
            // facet entry resource data that are used to set the pagination and would lead
            // to a /object endpoint
            halResource.add(buildLink(IanaLinkRelations.SELF.value(),
                    buildSearchFacetsBaseLink(content).build().toUriString()));

            PageImpl<SearchFacetEntryResource> page = new PageImpl<SearchFacetEntryResource>(
                    data, pageable, data.size());

            halResource.setPageHeader(new EmbeddedPageHeader(buildSearchBaseLink(content), page));

        }
    }

    protected Class<FacetsResource> getResourceClass() {
        return FacetsResource.class;
    }
}
