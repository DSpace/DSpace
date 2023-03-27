/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.IdentifierRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 *
 * Simple HAL wrapper for IdentifierRest model
 *
 * @author Kim Shepherd
 */
@RelNameDSpaceResource(IdentifierRest.NAME)
public class IdentifierResource extends DSpaceResource<IdentifierRest> {
    public IdentifierResource(IdentifierRest model, Utils utils) {
        super(model, utils);
    }
}
