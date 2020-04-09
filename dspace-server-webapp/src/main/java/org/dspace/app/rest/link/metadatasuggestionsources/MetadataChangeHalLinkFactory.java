/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.metadatasuggestionsources;

import java.util.LinkedList;

import org.dspace.app.rest.MetadataSuggestionsRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.MetadataChangeResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory takes care of adding links on the {@link MetadataChangeResource} resources
 */
@Component
public class MetadataChangeHalLinkFactory extends
                                          HalLinkFactory<MetadataChangeResource, MetadataSuggestionsRestController> {

    @Override
    protected void addLinks(MetadataChangeResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink(Link.REL_SELF, "placeholder"));

    }

    @Override
    protected Class<MetadataSuggestionsRestController> getControllerClass() {
        return MetadataSuggestionsRestController.class;
    }

    @Override
    protected Class<MetadataChangeResource> getResourceClass() {
        return MetadataChangeResource.class;
    }
}
