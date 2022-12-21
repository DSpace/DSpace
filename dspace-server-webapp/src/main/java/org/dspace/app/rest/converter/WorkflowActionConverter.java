/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.xmlworkflow.state.actions.WorkflowActionConfig;
import org.springframework.stereotype.Component;

/**
 * Converter to translate {@link WorkflowActionConfig} to a {@link WorkflowActionRest} object
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
@Component
public class WorkflowActionConverter implements DSpaceConverter<WorkflowActionConfig, WorkflowActionRest> {

    @Override
    public WorkflowActionRest convert(WorkflowActionConfig modelObject, Projection projection) {
        WorkflowActionRest restModel = new WorkflowActionRest();
        restModel.setProjection(projection);
        restModel.setId(modelObject.getId());
        restModel.setOptions(modelObject.getOptions());
        if (modelObject.isAdvanced()) {
            restModel.setAdvancedOptions(modelObject.getAdvancedOptions());
            restModel.setAdvancedInfo(modelObject.getAdvancedInfo());
        }
        return restModel;
    }

    @Override
    public Class<WorkflowActionConfig> getModelClass() {
        return WorkflowActionConfig.class;
    }
}
