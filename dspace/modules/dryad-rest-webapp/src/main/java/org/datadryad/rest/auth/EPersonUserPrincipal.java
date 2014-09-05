/*
 */
package org.datadryad.rest.auth;

import java.security.Principal;
import org.dspace.eperson.EPerson;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class EPersonUserPrincipal implements Principal {
    private final EPerson eperson;
    public EPersonUserPrincipal(EPerson eperson) {
        this.eperson = eperson;
    }

    @Override
    public String getName() {
        return eperson.getEmail();
    }

    public final Boolean hasRole(String role) {
        // TODO: come up with role scheme and determine if user is in it
        // e.g. CREATE_MANUSCRIPT_JOURNAL1
        return true;

    }

}
