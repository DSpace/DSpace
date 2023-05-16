/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.stream.Collectors;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.factory.XmlWorkflowFactory;
import org.dspace.xmlworkflow.state.Workflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Converter to translate Workflow to a Workflow Definition
 *
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@Component
public class WorkflowDefinitionConverter implements DSpaceConverter<Workflow, WorkflowDefinitionRest> {

    @Autowired
    protected XmlWorkflowFactory xmlWorkflowFactory;

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converter;

    @Override
    public WorkflowDefinitionRest convert(Workflow modelObject, Projection projection) {
        WorkflowDefinitionRest restModel = new WorkflowDefinitionRest();
        restModel.setName(modelObject.getID());
        restModel.setIsDefault(xmlWorkflowFactory.isDefaultWorkflow(modelObject.getID()));
        restModel.setProjection(projection);
        restModel.setSteps(modelObject.getSteps().stream()
            .map(x -> (WorkflowStepRest) converter.toRest(x, projection))
            .collect(Collectors.toList()));
        return restModel;
    }

    @Override
    public Class<Workflow> getModelClass() {
        return Workflow.class;
    }
}
