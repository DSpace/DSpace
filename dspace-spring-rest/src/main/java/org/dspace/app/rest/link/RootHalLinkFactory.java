/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.RootRestResourceController;
import org.dspace.app.rest.model.hateoas.RootResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by raf on 26/09/2017.
 */
@Component
public class RootHalLinkFactory extends HalLinkFactory<RootResource, RootRestResourceController> {

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    protected void addLinks(RootResource halResource, LinkedList<Link> list) {
        for(Link endpointLink : discoverableEndpointsService.getDiscoverableEndpoints()){
            list.add(buildLink(endpointLink.getRel(), halResource.getData().getDspaceRest() + endpointLink.getHref()));
        }
    }

    protected Class<RootRestResourceController> getControllerClass() {
        return RootRestResourceController.class;
    }

    protected Class<RootResource> getResourceClass() {
        return RootResource.class;
    }
}
