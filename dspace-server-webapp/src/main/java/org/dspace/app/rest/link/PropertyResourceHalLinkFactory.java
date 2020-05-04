/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;

import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.hateoas.PropertyResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class PropertyResourceHalLinkFactory extends HalLinkFactory<PropertyResource, RestResourceController> {
    @Override
    protected void addLinks(PropertyResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        halResource.removeLinks();
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<PropertyResource> getResourceClass() {
        return PropertyResource.class;
    }
}
