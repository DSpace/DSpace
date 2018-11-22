package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.dspace.app.rest.RelationshipRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.RelationshipResourceWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

@Component
public class RelationshipResourceWrapperHalLinkFactory
    extends HalLinkFactory<RelationshipResourceWrapper, RelationshipRestController> {
    protected void addLinks(RelationshipResourceWrapper halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        list.add(buildLink(Link.REL_SELF, getMethodOn()
            .retrieveByLabel(null, null, halResource.getContent().getLabel(), halResource.getContent().getDsoId())));
    }

    protected Class<RelationshipRestController> getControllerClass() {
        return RelationshipRestController.class;
    }

    protected Class<RelationshipResourceWrapper> getResourceClass() {
        return RelationshipResourceWrapper.class;
    }
}
