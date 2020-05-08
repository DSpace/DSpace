/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CrisTabLayoutRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * @author Danilo Di Nuzzo (danilo dot dinuzzo at 4science dot it)
 *
 */
@RelNameDSpaceResource(CrisTabLayoutRest.NAME)
public class CrisLayoutTabResource extends HALResource<CrisTabLayoutRest> {

    public CrisLayoutTabResource(CrisTabLayoutRest data) {
        super(data);
    }

}
