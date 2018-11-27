package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.dspace.app.rest.RelationshipRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.RelationshipRestWrapper;
import org.dspace.app.rest.model.hateoas.EmbeddedPageHeader;
import org.dspace.app.rest.model.hateoas.RelationshipResource;
import org.dspace.app.rest.model.hateoas.RelationshipResourceWrapper;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RelationshipResourceWrapperHalLinkFactory
    extends HalLinkFactory<RelationshipResourceWrapper, RelationshipRestController> {
    protected void addLinks(RelationshipResourceWrapper halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {

        PageImpl<RelationshipResource> page = new PageImpl<>(halResource.getFullList(), pageable,
                                                             halResource.getTotalElements());
        halResource.setPageHeader(new EmbeddedPageHeader(getSelfLink(halResource.getContent(), pageable),
                                                         page, true));
    }

    public String getSelfLink(RelationshipRestWrapper content, Pageable pageable) throws Exception {
        if (content != null) {
            UriComponentsBuilder uriBuilderSelfLink = uriBuilder(getMethodOn()
                                                                     .retrieveByLabel(null, null, content.getLabel(),
                                                                                      content.getDsoId(), pageable));
            return uriBuilderSelfLink.build().toString();
        }
        return null;
    }

    protected Class<RelationshipRestController> getControllerClass() {
        return RelationshipRestController.class;
    }

    protected Class<RelationshipResourceWrapper> getResourceClass() {
        return RelationshipResourceWrapper.class;
    }
}
