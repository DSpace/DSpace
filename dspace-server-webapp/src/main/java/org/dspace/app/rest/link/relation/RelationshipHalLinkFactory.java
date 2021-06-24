/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.hateoas.RelationshipResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class adds links to {@link org.dspace.app.rest.model.hateoas.RelationshipResource}s
 * This builds a link to both items included in the relationship
 */
@Component
public class RelationshipHalLinkFactory extends HalLinkFactory<RelationshipResource, RestResourceController> {
    @Override
    protected void addLinks(RelationshipResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        list.add(buildLink("leftItem", getMethodOn()
            .findOne(ItemRest.CATEGORY, English.plural(ItemRest.NAME), halResource.getContent().getLeftId())));

        list.add(buildLink("rightItem", getMethodOn()
            .findOne(ItemRest.CATEGORY, English.plural(ItemRest.NAME), halResource.getContent().getRightId())));
    }

    @Override
    protected Class<RestResourceController> getControllerClass() {
        return RestResourceController.class;
    }

    @Override
    protected Class<RelationshipResource> getResourceClass() {
        return RelationshipResource.class;
    }
}
