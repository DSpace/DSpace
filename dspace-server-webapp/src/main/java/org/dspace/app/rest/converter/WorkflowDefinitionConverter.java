/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.stereotype.Component;

/**
 * Converter to translate Workflow to a Workflow Definition
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component
public class WorkflowDefinitionConverter implements DSpaceConverter<Workflow, WorkflowDefinitionRest> {

    private Logger log = org.apache.logging.log4j.LogManager.getLogger(WorkflowDefinitionConverter.class);

    protected XmlWorkflowFactory xmlWorkflowFactory = XmlWorkflowServiceFactory.getInstance().getWorkflowFactory();

    @Override
    public WorkflowDefinitionRest convert(Workflow modelObject, Projection projection) {
        WorkflowDefinitionRest restModel = new WorkflowDefinitionRest();
        restModel.setName(modelObject.getID());
        try {
            restModel.setIsDefault(xmlWorkflowFactory.isDefaultWorkflow(modelObject.getID()));
        } catch (WorkflowConfigurationException e) {
            log.error("Error while trying to check if " + modelObject.getID() + " is the default workflow", e);
        }
        return restModel;
    }

    @Override
    public Class<Workflow> getModelClass() {
        return Workflow.class;
    }
}
