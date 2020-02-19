/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the rest repository responsible for managing WorkflowDefinition Rest objects
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component(WorkflowDefinitionRest.CATEGORY + "." + WorkflowDefinitionRest.NAME)
public class WorkflowDefinitionRestRepository extends DSpaceRestRepository<WorkflowDefinitionRest, String> {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    @Autowired
    private CollectionService collectionService;

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public WorkflowDefinitionRest findOne(Context context, String workflowName) {
        if (xmlWorkflowFactory.workflowByThisNameExists(workflowName)) {
            try {
                return converter.toRest(xmlWorkflowFactory.getWorkflowByName(workflowName), utils.obtainProjection());
            } catch (WorkflowConfigurationException e) {
                // Should never occur, since xmlWorkflowFactory.getWorkflowByName only throws a
                //      WorkflowConfigurationException if no workflow by that name is configured (tested earlier)
                throw new ResourceNotFoundException("No workflow with name " + workflowName + " is configured");
            }
        } else {
            throw new ResourceNotFoundException("No workflow with name " + workflowName + " is configured");
        }
    }

    @Override
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<WorkflowDefinitionRest> findAll(Context context, Pageable pageable) {
        List<Workflow> workflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
        return converter.toRestPage(utils.getPage(workflows, pageable), utils.obtainProjection());
    }

    /**
     * GET endpoint that returns the workflow definition that applies to a specific collection eventually fallback
     * to the default configuration.
     *
     * @param collectionId Uuid of the collection
     * @return the workflow definition for this collection
     */
    @SearchRestMethod(name = "findByCollection")
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public WorkflowDefinitionRest findByCollection(@Parameter(value = "uuid") UUID collectionId) throws SQLException {
        Context context = obtainContext();
        Collection collectionFromUuid = collectionService.find(context, collectionId);
        if (collectionFromUuid != null) {
            try {
                return converter.toRest(xmlWorkflowFactory.getWorkflow(collectionFromUuid), utils.obtainProjection());
            } catch (WorkflowConfigurationException e) {
                throw new ResourceNotFoundException("No workflow for this collection fault and " +
                        "no defaultWorkflow found");
            }
        } else {
            throw new ResourceNotFoundException("Collection with id " + collectionId + " not found");
        }
    }

    @Override
    public Class<WorkflowDefinitionRest> getDomainClass() {
        return WorkflowDefinitionRest.class;
    }
}
