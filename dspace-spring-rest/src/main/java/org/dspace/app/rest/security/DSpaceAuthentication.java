package org.dspace.app.rest.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class DSpaceAuthentication implements Authentication {

    private String username;
    private String password;
    private List<GrantedAuthority> authorities;
    private boolean authenticated = true;

    public DSpaceAuthentication (String username, String password, List<GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
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
}
