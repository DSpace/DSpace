/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.externalsources;

import java.util.LinkedList;

import org.dspace.app.rest.ExternalSourcesRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.ExternalSourceResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory adds links to the ExternalSourceResource object
 */
@Component
public class ExternalSourceHalLinkFactory extends
                                          HalLinkFactory<ExternalSourceResource, ExternalSourcesRestController> {

    @Override
    protected void addLinks(ExternalSourceResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink("entries", getMethodOn()
            .getExternalSourceEntries(halResource.getContent().getName(), "", null, null, null)));

    }

    @Override
    protected Class<ExternalSourcesRestController> getControllerClass() {
        return ExternalSourcesRestController.class;
    }

    @Override
    protected Class<ExternalSourceResource> getResourceClass() {
        return ExternalSourceResource.class;
    }
}
