/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.harvest;

import java.util.LinkedList;
import java.util.UUID;

import org.dspace.app.rest.model.HarvestedCollectionRest;
import org.dspace.app.rest.model.hateoas.HarvestedCollectionResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class adds links to {@link org.dspace.app.rest.model.hateoas.HarvestedCollectionResource}s
 * This builds a link to the collection harvest link
 */
@Component
public class HarvestedCollectionHalLinkFactory
    extends HarvestedCollectionRestHalLinkFactory<HarvestedCollectionResource> {

    protected void addLinks(HarvestedCollectionResource halResource, Pageable page, LinkedList<Link> list)
        throws Exception {
        HarvestedCollectionRest data = halResource.getContent();

        if (data != null) {
            list.add(
                buildLink(
                    Link.REL_SELF,
                    getMethodOn().get(UUID.fromString(data.getCollectionRest().getUuid()), null, null)
                )
            );
        }
    }

    protected Class<HarvestedCollectionResource> getResourceClass() {
        return HarvestedCollectionResource.class;
    }
}
