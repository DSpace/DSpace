/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by raf on 25/09/2017.
 */
@Component
public class DSpaceResourceHalLinkFactory extends HalLinkFactory<DSpaceResource, DiscoveryRestController> {


    protected void addLinks(DSpaceResource halResource, LinkedList<Link> list) {

    }

    protected Class<DiscoveryRestController> getControllerClass() {
        return null;
    }

    protected Class<DSpaceResource> getResourceClass() {
        return null;
    }

    protected String getSelfLink(DSpaceResource halResource) {
        return null;
    }

}
