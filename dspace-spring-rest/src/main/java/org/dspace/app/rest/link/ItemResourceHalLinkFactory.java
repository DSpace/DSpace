package org.dspace.app.rest.link;

import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.model.hateoas.ItemResource;
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
public class ItemResourceHalLinkFactory extends HalLinkFactory<ItemResource, DiscoveryRestController> {

    protected void addLinks(ItemResource halResource, LinkedList<Link> list) {

    }

    protected Class<DiscoveryRestController> getControllerClass() {
        return null;
    }

    protected Class<ItemResource> getResourceClass() {
        return null;
    }
}
