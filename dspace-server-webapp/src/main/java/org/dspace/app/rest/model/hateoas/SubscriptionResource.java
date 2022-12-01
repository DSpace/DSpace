/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * The Resource representation of a Subscription object
 */
@RelNameDSpaceResource(SubscriptionRest.NAME)
public class SubscriptionResource extends DSpaceResource<SubscriptionRest> {
    public SubscriptionResource(SubscriptionRest data, Utils utils) {
        super(data, utils);
    }
}
