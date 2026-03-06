/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SortOptionRest;

/**
 * The Resource representation of a {@link SortOptionRest} object.
 */
public class SortOptionResource extends HALResource<SortOptionRest> {

    public SortOptionResource(SortOptionRest content) {
        super(content);
    }
}
