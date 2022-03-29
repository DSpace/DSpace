/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * HAL resource for {@link RequestItemRest}.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@RelNameDSpaceResource(RequestItemRest.NAME)
public class RequestItemResource
        extends DSpaceResource<RequestItemRest> {
    public RequestItemResource(RequestItemRest item, Utils utils) {
        super(item, utils);
    }
}
