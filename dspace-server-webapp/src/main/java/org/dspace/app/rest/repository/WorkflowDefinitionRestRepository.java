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
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the rest repository responsible for managing WorkflowDefinition Rest objects
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component(WorkflowDefinitionRest.CATEGORY + "." + WorkflowDefinitionRest.NAME)
public class WorkflowDefinitionRestRepository extends DSpaceRestRepository<WorkflowDefinitionRest, String> {

    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    @Autowired
    private CollectionService collectionService;

    @Override
    public WorkflowDefinitionRest findOne(Context context, String s) {
        try {
            return converter.toRest(xmlWorkflowFactory.getWorkflowByName(s), utils.obtainProjection());
        } catch (WorkflowConfigurationException e) {
            // TODO ? Better exception?
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<WorkflowDefinitionRest> findAll(Context context, Pageable pageable) {
        try {
            List<Workflow> workflows = xmlWorkflowFactory.getAllConfiguredWorkflows();
            return converter.toRestPage(workflows, pageable, workflows.size(), utils.obtainProjection(true));
        } catch (WorkflowConfigurationException e) {
            // TODO ? Better exception?
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    /**
     * GET endpoint that returns the workflow definition that applies to a specific collection eventually fallback
     * to the default configuration.
     *
     * @param collectionId     Uuid of the collection
     * @return the workflow definition for this collection
     */
    @SearchRestMethod(name = "findByCollection")
    public WorkflowDefinitionRest findByCollection(@Parameter(value = "uuid") UUID collectionId) throws SQLException {
        try {
            Context context = obtainContext();
            Collection collectionFromUuid = collectionService.find(context, collectionId);
            if (collectionFromUuid != null) {
                return converter.toRest(xmlWorkflowFactory.getWorkflow(collectionFromUuid), utils.obtainProjection());
            } else {
                throw new ResourceNotFoundException("Collection with id " + collectionId + " not found");
            }
        } catch (WorkflowConfigurationException e) {
            // TODO ? Better exception?
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<WorkflowDefinitionRest> getDomainClass() {
        return WorkflowDefinitionRest.class;
    }
}
