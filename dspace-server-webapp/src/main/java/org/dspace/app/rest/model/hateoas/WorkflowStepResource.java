package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.WorkflowStepRest;
import org.dspace.app.rest.utils.Utils;

/**
 * {@link WorkflowStepRest} HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Maria Verdonck (Atmire) on 10/01/2020
 */
public class WorkflowStepResource extends DSpaceResource<WorkflowStepRest> {
    public WorkflowStepResource(WorkflowStepRest data, Utils utils) {
        super(data, utils);
    }
}
