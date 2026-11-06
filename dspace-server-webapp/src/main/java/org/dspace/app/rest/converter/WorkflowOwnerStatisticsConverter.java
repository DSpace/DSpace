/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.WorkflowOwnerStatisticsRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.statistics.WorkflowOwnerStatistics;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link DSpaceConverter} that converts
 * {@link WorkflowOwnerStatistics} to {@link WorkflowOwnerStatisticsRest}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class WorkflowOwnerStatisticsConverter
    implements DSpaceConverter<WorkflowOwnerStatistics, WorkflowOwnerStatisticsRest> {

    @Override
    public WorkflowOwnerStatisticsRest convert(WorkflowOwnerStatistics modelObject, Projection projection) {
        WorkflowOwnerStatisticsRest rest = new WorkflowOwnerStatisticsRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getOwner().getID().toString());
        rest.setName(modelObject.getOwnerName());
        rest.setEmail(modelObject.getOwner().getEmail());
        rest.setCount(modelObject.getCount());
        rest.setActionCounts(modelObject.getActionCounts());
        return rest;
    }

    @Override
    public Class<WorkflowOwnerStatistics> getModelClass() {
        return WorkflowOwnerStatistics.class;
    }

}
