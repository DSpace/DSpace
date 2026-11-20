/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.WorkflowStepStatisticsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * WorkflowStep statistics HAL Resource. The HAL Resource wraps the REST
 * Resource adding support for the links and embedded resources.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RelNameDSpaceResource(WorkflowStepStatisticsRest.NAME)
public class WorkflowStepStatisticsResource extends DSpaceResource<WorkflowStepStatisticsRest> {

    public WorkflowStepStatisticsResource(WorkflowStepStatisticsRest item, Utils utils) {
        super(item, utils);
    }

}
