/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.search;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.dspace.app.rest.model.hateoas.SearchSupportResource;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchSupportHalLinkFactory extends HalLinkFactory<SearchSupportResource, DiscoveryRestController> {

    protected void addLinks(SearchSupportResource halResource, LinkedList<Link> list) {
        list.add(buildLink(Link.REL_SELF, getMethodOn()
                .getSearchSupport(null, null)));
        list.add(buildLink("configuration", getMethodOn().getSearchConfiguration(null, null)));
        list.add(buildLink("facets", getMethodOn().getFacetsConfiguration(null, null)));
    }

    protected Class<SearchSupportResource> getResourceClass() {
        return SearchSupportResource.class;
    }

    protected String getSelfLink(SearchSupportResource halResource) {
        return null;
    }


    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
