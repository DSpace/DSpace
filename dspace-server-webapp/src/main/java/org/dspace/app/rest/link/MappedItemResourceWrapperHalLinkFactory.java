/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;

import org.dspace.app.rest.model.MappedItemRestWrapper;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.ItemResource;
import org.dspace.app.rest.model.hateoas.MappedItemResourceWrapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to add links to the MappedItemResourceWrapper objects
 */
@Component
public class MappedItemResourceWrapperHalLinkFactory
    extends MappedItemRestHalLinkFactory<MappedItemResourceWrapper> {
    protected void addLinks(MappedItemResourceWrapper halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        MappedItemRestWrapper mappingItemRestWrapper = halResource.getContent();
        if (mappingItemRestWrapper != null) {

            PageImpl<ItemResource> page = new PageImpl<>(halResource.getItemResources(), pageable,
                                                         halResource.getTotalElements());

            halResource.setPageHeader(new EmbeddedPageHeader(getSelfLink(mappingItemRestWrapper, pageable),
                                                             page, true));
        }

    }
    private String getSelfLink(MappedItemRestWrapper mappingItemRestWrapper, Pageable pageable) throws Exception {
        if (mappingItemRestWrapper != null) {
            UriComponentsBuilder uriBuilderSelfLink = uriBuilder(getMethodOn()
                                                                     .retrieve(
                                                                         mappingItemRestWrapper.getCollectionUuid(),
                                                                         null, null, pageable));
            return uriBuilderSelfLink.build().toString();
        }
        return null;

    }

    protected Class<MappedItemResourceWrapper> getResourceClass() {
        return MappedItemResourceWrapper.class;
    }
}
