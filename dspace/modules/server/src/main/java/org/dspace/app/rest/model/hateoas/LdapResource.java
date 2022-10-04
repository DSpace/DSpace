package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.LdapRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * LdapRest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 */
@RelNameDSpaceResource(LdapRest.NAME)
public class LdapResource extends HALResource<LdapRest> {
    public LdapResource(LdapRest ldapRest) {
        super(ldapRest);
    }
}
