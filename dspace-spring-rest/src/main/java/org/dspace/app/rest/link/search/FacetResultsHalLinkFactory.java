/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.FacetResultsRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.FacetResultsResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This factory provides a means to add links to the FacetResultsResource class. This method will be called when the
 * addLinks method on the HalLinkService is called if the Resource given is valid
 */
@Component
public class FacetResultsHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetResultsResource> {

    @Override
    protected void addLinks(FacetResultsResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        FacetResultsRest data = halResource.getContent();

        if (data != null && pageable != null) {
            PageImpl page = new PageImpl<>(data.getFacetResultList(), pageable,
                                           pageable.getOffset() + data.getFacetResultList().size() + (data
                                               .getFacetEntry().isHasMore() ? 1 : 0));

            halResource.setPageHeader(new EmbeddedPageHeader(buildFacetBaseLink(data), page, false));
        }

    }

    @Override
    protected Class<FacetResultsResource> getResourceClass() {
        return FacetResultsResource.class;
    }

}
