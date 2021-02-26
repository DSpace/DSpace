/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RootRest;

/**
 * The purpose of this class is to wrap the information of the RootRest into a HAL resource
 */
public class RootResource extends HALResource<RootRest> {

    public RootResource(RootRest rootRest) {
        super(rootRest);
    }

}
