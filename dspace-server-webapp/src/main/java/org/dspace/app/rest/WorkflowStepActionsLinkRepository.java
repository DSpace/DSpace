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

import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.AbstractDSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "actions" subresource of an individual workflow step.
 *
 * @author Maria Verdonck (Atmire) on 24/02/2020
 */
@Component(WorkflowStepRest.CATEGORY + "." + WorkflowStepRest.NAME + "."
    + WorkflowStepRest.ACTIONS)
public class WorkflowStepActionsLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    /**
     * GET endpoint that returns the list of actions of a workflow step.
     *
     * @param request           The request object
     * @param workflowStepName  Name of workflow step we want the actions from
     * @return List of actions of the requested workflow step
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<WorkflowActionRest> getActions(@Nullable HttpServletRequest request,
                                               String workflowStepName,
                                               @Nullable Pageable optionalPageable,
                                               Projection projection) {
        List<WorkflowActionConfig> actions = xmlWorkflowFactory.getStepByName(workflowStepName).getActions();
        Pageable pageable = optionalPageable != null ? optionalPageable : new PageRequest(0, 20);
        return converter.toRestPage(utils.getPage(actions, pageable), projection);
    }
}
