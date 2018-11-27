/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.dspace.app.rest.RelationshipRestController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RelationshipRestWrapper;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
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

        PageImpl<RelationshipRest> page = new PageImpl<>(halResource.getContent().getRelationshipRestList(), pageable,
                                                         halResource.getContent().getRelationshipRestList().size());

        halResource.setPageHeader(new EmbeddedPage(getSelfLink(halResource.getContent(), pageable),
                                                   page, halResource.getContent().getRelationshipRestList(), true, "relationships"));
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
