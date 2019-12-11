package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * This is the rest repository responsible for managing WorkflowDefinition Rest objects
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component(WorkflowDefinitionRest.CATEGORY + "." + WorkflowDefinitionRest.NAME)
public class WorkflowDefinitionRestRepository extends DSpaceRestRepository<WorkflowDefinitionRest, String> {

    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

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

    @Override
    public Class<WorkflowDefinitionRest> getDomainClass() {
        return WorkflowDefinitionRest.class;
    }
}
