/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.AuthenticationStatusRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Status Resource, wraps the status object and the authenticated EPerson
 *
 * @author Atmire NV (info at atmire dot com)
 */
@RelNameDSpaceResource(AuthenticationStatusRest.NAME)
public class AuthenticationStatusResource extends DSpaceResource<AuthenticationStatusRest> {
    public AuthenticationStatusResource(AuthenticationStatusRest data, Utils utils, String... rels) {
        super(data, utils, rels);
    }
}
