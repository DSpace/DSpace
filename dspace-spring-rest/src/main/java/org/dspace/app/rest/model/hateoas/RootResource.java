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
 * Created by raf on 26/09/2017.
 */
public class RootResource extends HALResource<RootRest> {

    public RootResource(RootRest rootRest) {
        super(rootRest);
    }

}
