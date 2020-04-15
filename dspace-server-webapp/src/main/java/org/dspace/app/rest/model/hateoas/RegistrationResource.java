/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RegistrationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

@RelNameDSpaceResource(RegistrationRest.NAME)
public class RegistrationResource extends HALResource<RegistrationRest> {
    public RegistrationResource(RegistrationRest content) {
        super(content);
    }
}
