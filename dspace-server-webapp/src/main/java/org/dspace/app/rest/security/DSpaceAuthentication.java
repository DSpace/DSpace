/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.dspace.eperson.EPerson;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom Authentication for use with DSpace
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class DSpaceAuthentication implements Authentication {


    private Date previousLoginDate;
    private String username;
    private String password;
    private List<GrantedAuthority> authorities;
    private boolean authenticated;

    /**
     * Create a DSpaceAuthentication instance for an already authenticated EPerson, including their GrantedAuthority
     * objects.
     * <P>
     * NOTE: This type of DSpaceAuthentication object is returned to Spring after a successful authentication.
     * @param ePerson authenticated EPerson
     * @param authorities EPerson's authorities
     */
    public DSpaceAuthentication(EPerson ePerson, List<GrantedAuthority> authorities) {
        this.previousLoginDate = ePerson.getPreviousActive();
        this.username = ePerson.getEmail();
        this.authorities = authorities;
        this.authenticated = true;
    }

    /**
     * Create a temporary DSpaceAuthentication instance which may be used to store information about the user who will
     * be attempting authentication.
     * <P>
     * NOTE: This type of DSpaceAuthentication object is used to attempt a new authentication in DSpace. It is therefore
     * temporary in nature, as it will be discarded after successful authentication.
     * @param username username to attempt authentication for
     * @param password password to use for authentication
     */
    public DSpaceAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
        this.authenticated = false;
    }

    /**
     * Create a temporary, empty DSpaceAuthentication instance which may be used to trigger an implicit authentication.
     * An example is Shibboleth, as this doesn't require an explicit username/password, as the user will have been
     * authenticated externally, and DSpace just needs to perform an implicit authentication by looking for the auth
     * data passed to it by Shibboleth.
     */
    public DSpaceAuthentication() {
        // Initialize with a 'null' username and password
        this(null, (String) null);
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Object getCredentials() {
        return password;
    }

    public Object getDetails() {
        return null;
    }

    public Object getPrincipal() {
        return username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        this.authenticated = authenticated;
    }

    public String getName() {
        return username;
    }

    public Date getPreviousLoginDate() {
        return previousLoginDate;
    }
}
