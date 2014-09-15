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
        // Not using role-based security
        return Boolean.FALSE;
    }

    public final Integer getID() {
        if(eperson != null) {
            return eperson.getID();
        } else {
            return null;
        }
    }

}
