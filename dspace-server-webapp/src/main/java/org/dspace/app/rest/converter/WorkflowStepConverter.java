/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.stream.Collectors;

import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.state.Step;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Converter to translate {@link Step} to a {@link WorkflowStepRest} object
 *
 * @author Maria Verdonck (Atmire) on 10/01/2020
 */
@Component
public class WorkflowStepConverter implements DSpaceConverter<Step, WorkflowStepRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    ConverterService converter;

    @Override
    public WorkflowStepRest convert(Step modelObject, Projection projection) {
        WorkflowStepRest restModel = new WorkflowStepRest();
        restModel.setProjection(projection);
        restModel.setId(modelObject.getId());
        restModel.setWorkflowactions(modelObject.getActions().stream()
            .map(x -> (WorkflowActionRest) converter.toRest(x, projection))
            .collect(Collectors.toList()));
        return restModel;
    }

    @Override
    public Class<Step> getModelClass() {
        return Step.class;
    }
}
