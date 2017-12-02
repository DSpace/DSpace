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
 * @author Atmire NV (info at atmire dot com)
 */
public class DSpaceAuthentication implements Authentication {


    private Date previousLoginDate;
    private String username;
    private String password;
    private List<GrantedAuthority> authorities;
    private boolean authenticated = true;


    public DSpaceAuthentication (EPerson ePerson, List<GrantedAuthority> authorities) {
        this.previousLoginDate = ePerson.getPreviousActive();
        this.username = ePerson.getEmail();
        this.authorities = authorities;
    }

    public DSpaceAuthentication (String username, String password, List<GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public DSpaceAuthentication (String username, List<GrantedAuthority> authorities) {
        this(username, null, authorities);
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
