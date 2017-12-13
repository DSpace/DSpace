/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import org.dspace.app.rest.model.SearchResultsRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.SearchResultEntryResource;
import org.dspace.app.rest.model.hateoas.SearchResultsResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.LinkedList;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchResultsResourceHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchResultsResource> {

    protected void addLinks(SearchResultsResource halResource, Pageable pageable, LinkedList<Link> list) {
        SearchResultsRest data = halResource.getContent();

        if(data != null && pageable != null){
            PageImpl<SearchResultEntryResource> page = new PageImpl<SearchResultEntryResource>(halResource.getEntryResources(),
                    pageable, data.getTotalNumberOfResults());

            halResource.setPageHeader(new EmbeddedPageHeader(buildSearchBaseLink(data), page));
        }
    }

    @Override
    protected Class<SearchResultsResource> getResourceClass() {
        return SearchResultsResource.class;
    }


}