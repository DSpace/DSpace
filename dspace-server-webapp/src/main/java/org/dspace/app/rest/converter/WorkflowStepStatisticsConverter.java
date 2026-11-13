/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkflowStepStatisticsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.statistics.WorkflowStepStatistics;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts
 * {@link WorkflowStepStatistics} to {@link WorkflowStepStatisticsRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class WorkflowStepStatisticsConverter
    implements DSpaceConverter<WorkflowStepStatistics, WorkflowStepStatisticsRest> {

    @Override
    public WorkflowStepStatisticsRest convert(WorkflowStepStatistics modelObject, Projection projection) {
        WorkflowStepStatisticsRest rest = new WorkflowStepStatisticsRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getStepName());
        rest.setName(modelObject.getStepName());
        rest.setCount(modelObject.getCount());
        rest.setActionCounts(modelObject.getActionCounts());
        return rest;
    }

    @Override
    public Class<WorkflowStepStatistics> getModelClass() {
        return WorkflowStepStatistics.class;
    }

}
