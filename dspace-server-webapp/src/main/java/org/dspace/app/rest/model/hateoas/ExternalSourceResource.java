/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ExternalSourceRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class serves as the HAL Resource for the ExternalSourceRest object
 */
@RelNameDSpaceResource(ExternalSourceRest.NAME)
public class ExternalSourceResource extends DSpaceResource<ExternalSourceRest> {
    public ExternalSourceResource(ExternalSourceRest externalSourceRest, Utils utils) {
        super(externalSourceRest, utils);
    }
}
