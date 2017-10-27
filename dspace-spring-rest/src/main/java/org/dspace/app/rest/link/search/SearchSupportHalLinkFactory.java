/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import java.util.LinkedList;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchSupportHalLinkFactory extends HalLinkFactory<SearchSupportResource, DiscoveryRestController> {

    protected void addLinks(SearchSupportResource halResource, Pageable pageable, LinkedList<Link> list) {
        list.add(buildLink(Link.REL_SELF, getMethodOn()
                .getSearchSupport(null, null)));
        list.add(buildLink("search", getMethodOn().getSearchConfiguration(null, null)));
        list.add(buildLink("facets", getMethodOn().getFacetsConfiguration(null, null)));
    }

    protected Class<SearchSupportResource> getResourceClass() {
        return SearchSupportResource.class;
    }

    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
