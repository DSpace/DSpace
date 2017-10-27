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
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.LinkedList;


/**
 * Created by raf on 25/09/2017.
 */
@Component
public class SearchConfigurationResourceHalLinkFactory extends HalLinkFactory<SearchConfigurationResource, DiscoveryRestController> {

    protected void addLinks(SearchConfigurationResource halResource, Pageable pageable, LinkedList<Link> list) {
        SearchConfigurationRest data = halResource.getData();

        if(data != null){

            list.add(buildLink(Link.REL_SELF, getMethodOn()
                    .getSearchConfiguration(data.getScope(), data.getConfigurationName())));
            list.add(buildLink("objects", getMethodOn().getSearchObjects(null, null, null, null, null, null)));
        }
    }

    protected Class<SearchConfigurationResource> getResourceClass() {
        return SearchConfigurationResource.class;
    }

    protected Class<DiscoveryRestController> getControllerClass() {
        return DiscoveryRestController.class;
    }

}
