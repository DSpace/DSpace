/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;

import org.dspace.app.rest.model.MappingCollectionRestWrapper;
import org.dspace.app.rest.model.hateoas.MappingCollectionResourceWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This class' purpose is to add links to the MappingCollectionResourceWrapper objects
 */
@Component
public class MappingCollectionResourceWrapperHalLinkFactory
    extends MappingCollectionRestHalLinkFactory<MappingCollectionResourceWrapper> {

    protected void addLinks(MappingCollectionResourceWrapper halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        MappingCollectionRestWrapper mappingCollectionRestWrapper = halResource.getContent();
        if (mappingCollectionRestWrapper != null) {

            UriComponentsBuilder uriBuilderSelfLink = uriBuilder(getMethodOn()
                                                                     .retrieve(
                                                                         mappingCollectionRestWrapper.getItem().getID(),
                                                                         null, null, null));
            list.add(buildLink(Link.REL_SELF, uriBuilderSelfLink.build().toString()));
        }
    }


    protected Class<MappingCollectionResourceWrapper> getResourceClass() {
        return MappingCollectionResourceWrapper.class;
    }
}
