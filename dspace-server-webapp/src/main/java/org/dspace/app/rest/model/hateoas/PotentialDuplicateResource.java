/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.PotentialDuplicateRest;

/**
 *
 * Wrap PotentialDuplicatesRest REST resource in a very simple HALResource class
 *
 * @author Kim Shepherd <kim@shepherd.nz>
 */
public class PotentialDuplicateResource extends HALResource<PotentialDuplicateRest> {
    public PotentialDuplicateResource(PotentialDuplicateRest data) {
        super(data);
    }
}
