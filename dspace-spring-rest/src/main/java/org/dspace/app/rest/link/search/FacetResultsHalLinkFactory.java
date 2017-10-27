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
 * Created by tom on 27/10/2017.
 */
@Component
public class FacetResultsHalLinkFactory extends DiscoveryRestHalLinkFactory<FacetResultsResource> {

    @Override
    protected void addLinks(FacetResultsResource halResource, Pageable pageable, LinkedList<Link> list) {
        FacetResultsRest data = halResource.getContent();

        if(data != null && pageable != null){
            PageImpl page = new PageImpl<>(data.getFacetResultList(), data.getPage(),
                    data.getFacetResultList().size() + (data.getFacetEntry().isHasMore() ? 1 : 0));

            halResource.setPageHeader(new EmbeddedPageHeader(buildFacetBaseLink(data), page, false));
        }
    }

    @Override
    protected Class<FacetResultsResource> getResourceClass() {
        return FacetResultsResource.class;
    }

}
