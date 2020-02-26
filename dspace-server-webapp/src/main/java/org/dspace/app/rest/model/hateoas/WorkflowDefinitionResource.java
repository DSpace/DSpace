/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.WorkflowDefinitionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * WorkflowDefinition Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 * @author Maria Verdonck (Atmire) on 11/12/2019
 */
@RelNameDSpaceResource(WorkflowDefinitionRest.NAME)
public class WorkflowDefinitionResource extends DSpaceResource<WorkflowDefinitionRest> {
    public WorkflowDefinitionResource(WorkflowDefinitionRest data, Utils utils) {
        super(data, utils);
    }
}
