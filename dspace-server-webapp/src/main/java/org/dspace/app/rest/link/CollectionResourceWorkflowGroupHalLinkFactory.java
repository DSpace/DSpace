/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import org.dspace.app.rest.CollectionGroupRestController;
import org.dspace.app.rest.model.hateoas.CollectionResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.xmlworkflow.Role;
import org.dspace.xmlworkflow.WorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class makes sure that the workflowGroup links with the relevant WorkflowRoles are added
 * to the CollectionResource in a dynamic way depending on what workflow is enabled for the collection
 */
@Component
public class CollectionResourceWorkflowGroupHalLinkFactory
    extends HalLinkFactory<CollectionResource, CollectionGroupRestController> {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private RequestService requestService;

    @Override
    protected void addLinks(CollectionResource halResource, Pageable pageable, LinkedList<Link> list) throws Exception {

        Context context = ContextUtil.obtainContext(requestService.getCurrentRequest().getHttpServletRequest());
        Collection collection = collectionService.find(context, UUID.fromString(halResource.getContent().getId()));
        Map<String, Role> roles = WorkflowUtils.getCollectionRoles(collection);
        UUID resourceUuid = UUID.fromString(halResource.getContent().getUuid());
        for (Map.Entry<String, Role> entry : roles.entrySet()) {
            list.add(buildLink("workflowGroups", getMethodOn()
                .getWorkflowGroupForRole(resourceUuid, null, null,
                                         entry.getKey())).withName(entry.getKey()));
        }
    }

    @Override
    protected Class<CollectionGroupRestController> getControllerClass() {
        return CollectionGroupRestController.class;
    }

    @Override
    protected Class<CollectionResource> getResourceClass() {
        return CollectionResource.class;
    }
}
