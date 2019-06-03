/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.model.SearchResultEntryRest;
import org.dspace.app.rest.model.hateoas.SearchResultEntryResource;
import org.dspace.app.rest.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This factory class provides a means to add links to the SearchResultsEntryResource. This method will be called
 * from the
 * HalLinkService addLinks method if the HalResource given is eligible.
 */
@Component
public class SearchResultEntryHalLinkFactory extends DiscoveryRestHalLinkFactory<SearchResultEntryResource> {

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
    protected Class<SearchResultEntryResource> getResourceClass() {
        return SearchResultEntryResource.class;
    }

}
