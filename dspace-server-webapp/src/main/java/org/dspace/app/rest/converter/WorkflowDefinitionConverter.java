package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.stereotype.Component;

/**
 * Converter to translate Workflow to a Workflow Definition
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component
public class WorkflowDefinitionConverter implements DSpaceConverter<Workflow, WorkflowDefinitionRest> {

    @Override
    public WorkflowDefinitionRest convert(Workflow modelObject, Projection projection) {
        WorkflowDefinitionRest restModel = new WorkflowDefinitionRest();
        restModel.setName(modelObject.getID());
        restModel.setDefault(modelObject.getID().equalsIgnoreCase("default"));
        return restModel;
    }

    @Override
    public Class<Workflow> getModelClass() {
        return Workflow.class;
    }
}
