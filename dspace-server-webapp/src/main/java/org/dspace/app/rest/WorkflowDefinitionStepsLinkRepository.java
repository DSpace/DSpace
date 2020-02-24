/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "steps" subresource of an individual workflow definition.
 *
 * @author Maria Verdonck (Atmire) on 24/02/2020
 */
@Component(WorkflowDefinitionRest.CATEGORY + "." + WorkflowDefinitionRest.NAME + "."
    + WorkflowDefinitionRest.STEPS)
public class WorkflowDefinitionStepsLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    /**
     * GET endpoint that returns the list of steps of a workflow-definition.
     *
     * @param request      The request object
     * @param workflowName Name of workflow we want the steps from
     * @return List of steps of the requested workflow
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<WorkflowStepRest> getSteps(@Nullable HttpServletRequest request,
                                                String workflowName,
                                                @Nullable Pageable optionalPageable,
                                                Projection projection) {
        try {
            List<Step> steps = xmlWorkflowFactory.getWorkflowByName(workflowName).getSteps();
            Pageable pageable = optionalPageable != null ? optionalPageable : new PageRequest(0, 20);
            return converter.toRestPage(utils.getPage(steps, pageable), projection);
        } catch (WorkflowConfigurationException e) {
            throw new ResourceNotFoundException("No workflow with name " + workflowName + " is configured");
        }
    }
}
