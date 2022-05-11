/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.AccessStatusRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * Access Status Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 */
@RelNameDSpaceResource(AccessStatusRest.NAME)
public class AccessStatusResource extends HALResource<AccessStatusRest> {

    @JsonUnwrapped
    private AccessStatusRest data;

    public AccessStatusResource(AccessStatusRest entry) {
        super(entry);
    }

    public AccessStatusRest getData() {
        return data;
    }
}
