/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the rest repository responsible for managing {@link WorkflowActionRest} objects
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
@Component(WorkflowActionRest.CATEGORY + "." + WorkflowActionRest.NAME)
public class WorkflowActionRestRepository extends DSpaceRestRepository<WorkflowActionRest, String> {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public WorkflowActionRest findOne(Context context, String workflowActionName) {
        WorkflowActionConfig actionConfig = this.xmlWorkflowFactory.getActionByName(workflowActionName);
        if (actionConfig != null) {
            return converter.toRest(actionConfig, utils.obtainProjection());
        } else {
            throw new ResourceNotFoundException("No workflow action with name " + workflowActionName
                    + " is configured");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<WorkflowActionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(WorkflowActionRest.NAME, "findAll");
    }

    @Override
    public Class<WorkflowActionRest> getDomainClass() {
        return WorkflowActionRest.class;
    }
}
