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
import org.dspace.app.rest.model.hateoas.MetadataSuggestionsSourceResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory adds links to the MetadataSuggestionsSourceResource object
 */
@Component
public class MetadataSuggestionsSourceHalLinkFactory
    extends HalLinkFactory<MetadataSuggestionsSourceResource, MetadataSuggestionsRestController> {

    @Override
    protected void addLinks(MetadataSuggestionsSourceResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        list.add(buildLink("entries", getMethodOn()
            .getMetadataSugggestionEntries(pageable, null, null, halResource.getContent().getName(),
                                           null, null, null, null, null, null)));


    }

    @Override
    protected Class<MetadataSuggestionsRestController> getControllerClass() {
        return MetadataSuggestionsRestController.class;
    }

    @Override
    protected Class<MetadataSuggestionsSourceResource> getResourceClass() {
        return MetadataSuggestionsSourceResource.class;
    }
}
