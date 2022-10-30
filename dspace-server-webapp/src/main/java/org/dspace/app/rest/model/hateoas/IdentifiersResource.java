/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.IdentifiersRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Boilerplate hateos resource for IdentifiersRest
 *
 * @author Kim Shepherd
 */
@RelNameDSpaceResource(IdentifiersRest.NAME)
public class IdentifiersResource extends HALResource<IdentifiersRest> {
    public IdentifiersResource(IdentifiersRest data, Utils utils) {
        super(data);
    }
}
