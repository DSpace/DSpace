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
import org.dspace.app.rest.model.hateoas.MetadataSuggestionEntryResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This HalLinkFactory will implement links on the {@link MetadataSuggestionEntryResource} object
 */
@Component
public class MetadataSuggestionEntryHalLinkFactory
    extends HalLinkFactory<MetadataSuggestionEntryResource, MetadataSuggestionsRestController> {

    @Override
    protected void addLinks(MetadataSuggestionEntryResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {


        list.add(buildLink(Link.REL_SELF, getMethodOn()
            .getMetadataSuggestionEntry(halResource.getContent().getMetadataSuggestion(),
                                        halResource.getContent().getId(), null, null, null, null)));
        list.add(buildLink("changes", getMethodOn()
            .getMetadataSuggestionEntryChanges(halResource.getContent().getMetadataSuggestion(),
                                               halResource.getContent().getId(), null, null, null, null)));

    }

    @Override
    protected Class<MetadataSuggestionsRestController> getControllerClass() {
        return MetadataSuggestionsRestController.class;
    }

    @Override
    protected Class<MetadataSuggestionEntryResource> getResourceClass() {
        return MetadataSuggestionEntryResource.class;
    }
}
