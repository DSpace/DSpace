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
import org.dspace.app.rest.model.SortOptionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

@RelNameDSpaceResource(SortOptionRest.NAME)
public class SortOptionResource extends HALResource<SortOptionRest> {

    @JsonUnwrapped
    private AccessStatusRest data;

    public SortOptionResource(SortOptionRest content) {
        super(content);
    }

    public AccessStatusRest getData() {
        return data;
    }
}
