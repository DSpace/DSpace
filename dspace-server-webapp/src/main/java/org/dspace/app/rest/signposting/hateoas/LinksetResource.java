/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.hateoas;

import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.utils.Utils;

/**
 * Linkset Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
@RelNameDSpaceResource(LinksetRest.NAME)
public class LinksetResource extends DSpaceResource<LinksetRest> {
    public LinksetResource(LinksetRest linkset, Utils utils) {
        super(linkset, utils);
    }
}
