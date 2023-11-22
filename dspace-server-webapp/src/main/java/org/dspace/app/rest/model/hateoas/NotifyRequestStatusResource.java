/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.NotifyRequestStatusRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * NotifyRequestStatus Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 */
@RelNameDSpaceResource(NotifyRequestStatusRest.NAME)
public class NotifyRequestStatusResource extends DSpaceResource<NotifyRequestStatusRest> {
    public NotifyRequestStatusResource(NotifyRequestStatusRest status, Utils utils) {
        super(status, utils);
    }
}
