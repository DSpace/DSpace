/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.externalsources;

import static org.springframework.hateoas.IanaLinkRelations.SELF;

import java.util.LinkedList;

import org.dspace.app.rest.ExternalSourcesRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.ExternalSourceEntryResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory adds links to the ExternalSourceEntryResource object
 */
@Component
public class ExternalSourceEntryHalLinkFactory
    extends HalLinkFactory<ExternalSourceEntryResource, ExternalSourcesRestController> {

    @Override
    protected void addLinks(ExternalSourceEntryResource halResource, Pageable pageable, LinkedList<Link> list)
            throws Exception {
        String externalSource = halResource.getContent().getExternalSource();
        String id = halResource.getContent().getId();
        list.add(buildLink(getMethodOn().getExternalSourceEntryValue(externalSource, id), SELF.value()));
    }

    @Override
    protected Class<ExternalSourcesRestController> getControllerClass() {
        return ExternalSourcesRestController.class;
    }

    protected Class<ExternalSourceEntryResource> getResourceClass() {
        return ExternalSourceEntryResource.class;
    }
}
