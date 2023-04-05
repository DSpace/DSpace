/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * WorkspaceItem Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RelNameDSpaceResource(WorkspaceItemRest.NAME)
public class WorkspaceItemResource extends DSpaceResource<WorkspaceItemRest> {
    public WorkspaceItemResource(WorkspaceItemRest witem, Utils utils) {
        super(witem, utils);
    }
}
