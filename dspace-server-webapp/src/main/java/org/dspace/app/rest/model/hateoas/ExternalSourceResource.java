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

/**
 * This class serves as the HAL Resource for the ExternalSourceRest object
 */
@RelNameDSpaceResource(ExternalSourceRest.NAME)
public class ExternalSourceResource extends HALResource<ExternalSourceRest> {
    public ExternalSourceResource(ExternalSourceRest content) {
        super(content);
    }
}
