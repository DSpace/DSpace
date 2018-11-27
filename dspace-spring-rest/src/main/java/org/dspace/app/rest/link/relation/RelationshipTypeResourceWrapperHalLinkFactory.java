/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.dspace.app.rest.RelationshipTypeRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.RelationshipTypeResourceWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class RelationshipTypeResourceWrapperHalLinkFactory
    extends HalLinkFactory<RelationshipTypeResourceWrapper, RelationshipTypeRestController> {
    protected void addLinks(RelationshipTypeResourceWrapper halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        list.add(buildLink(Link.REL_SELF, getMethodOn().retrieve(halResource.getContent().getEntityTypeId(), null, null)));
    }

    protected Class<RelationshipTypeRestController> getControllerClass() {
        return RelationshipTypeRestController.class;
    }

    protected Class<RelationshipTypeResourceWrapper> getResourceClass() {
        return RelationshipTypeResourceWrapper.class;
    }
}
