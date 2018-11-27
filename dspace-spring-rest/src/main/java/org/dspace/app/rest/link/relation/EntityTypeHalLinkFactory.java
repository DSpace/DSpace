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
import org.dspace.app.rest.model.hateoas.EntityTypeResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class EntityTypeHalLinkFactory extends HalLinkFactory<EntityTypeResource, RelationshipTypeRestController> {
    protected void addLinks(EntityTypeResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {
        list.add(buildLink("relationshiptypes", getMethodOn().retrieve(halResource.getContent().getId(), null, null)));
    }

    protected Class<RelationshipTypeRestController> getControllerClass() {
        return RelationshipTypeRestController.class;
    }

    protected Class<EntityTypeResource> getResourceClass() {
        return EntityTypeResource.class;
    }
}
