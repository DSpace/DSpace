/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link.relation;

import java.util.LinkedList;

import org.dspace.app.rest.WorkflowDefinitionController;
import org.dspace.app.rest.link.HalLinkFactory;
import org.dspace.app.rest.model.hateoas.WorkflowDefinitionResource;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to add the links to the WorkflowDefinitionResource. This function and class will be called
 * and used when the HalLinkService addLinks methods is called as it'll iterate over all the different factories and
 * check whether these are allowed to create links for said resource or not.
 *
 * @author Maria Verdonck (Atmire) on 14/01/2020
 */
@Component
public class WorkflowDefinitionHalLinkFactory extends HalLinkFactory<WorkflowDefinitionResource,
    WorkflowDefinitionController> {

    @Override
    protected void addLinks(WorkflowDefinitionResource halResource, Pageable pageable, LinkedList<Link> list)
        throws Exception {
        list.add(buildLink("collections", getMethodOn()
            .getCollections(null, halResource.getContent().getId(), pageable)));
        list.add(buildLink("steps", getMethodOn()
            .getSteps(null, halResource.getContent().getId(), pageable)));
    }

    @Override
    protected Class<WorkflowDefinitionController> getControllerClass() {
        return WorkflowDefinitionController.class;
    }

    @Override
    protected Class<WorkflowDefinitionResource> getResourceClass() {
        return WorkflowDefinitionResource.class;
    }
}
