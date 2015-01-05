/*
 */
package org.datadryad.rest.auth;

import java.security.Principal;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class EPersonSecurityContext implements SecurityContext{

    private final EPersonUserPrincipal userPrincipal;
    public EPersonSecurityContext(EPersonUserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return userPrincipal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }

}
