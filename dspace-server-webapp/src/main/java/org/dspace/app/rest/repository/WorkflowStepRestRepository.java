/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the rest repository responsible for managing {@link WorkflowStepRest} objects
 *
 * @author Maria Verdonck (Atmire) on 10/01/2020
 */
@Component(WorkflowStepRest.CATEGORY + "." + WorkflowStepRest.NAME)
public class WorkflowStepRestRepository extends DSpaceRestRepository<WorkflowStepRest, String> {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public WorkflowStepRest findOne(Context context, String workflowStepName) {
        Step step = this.xmlWorkflowFactory.getStepByName(workflowStepName);
        if (step != null) {
            return converter.toRest(step, utils.obtainProjection());
        } else {
            throw new ResourceNotFoundException("No workflow step with name " + workflowStepName
                + " is configured");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<WorkflowStepRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(WorkflowStepRest.NAME, "findAll");
    }

    @Override
    public Class<WorkflowStepRest> getDomainClass() {
        return WorkflowStepRest.class;
    }
}
