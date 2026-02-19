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
import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.hateoas.SearchResultEntryResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class will add links to the SearchResultsResource. This method will be called when calling the higher up
 * addLinks in the HalLinkService
 */
@Component
public class SearchResultEntryHalLinkFactory extends HalLinkFactory<SearchResultEntryResource, RestResourceController> {
    @Autowired
    private Utils utils;

    @Override
    protected void addLinks(SearchResultEntryResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        SearchResultEntryRest data = halResource.getContent();

        if (data != null && data.getIndexableObject() != null) {
            list.add(utils.linkToSingleResource(data.getIndexableObject(),
                SearchResultEntryResource.INDEXABLE_OBJECT_LINK));
        }
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<SearchResultEntryResource> getResourceClass() {
        return SearchResultEntryResource.class;
    }
}
