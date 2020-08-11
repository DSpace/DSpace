/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.WorkflowActionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * {@link WorkflowActionRest} HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Maria Verdonck (Atmire) on 06/01/2020
 */
@RelNameDSpaceResource(WorkflowActionRest.NAME)
public class WorkflowActionResource extends DSpaceResource<WorkflowActionRest> {
    public WorkflowActionResource(WorkflowActionRest data, Utils utils) {
        super(data, utils);
    }
}
