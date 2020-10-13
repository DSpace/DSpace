/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * This class will filter Shibboleth requests to see if the user has been authenticated via Shibboleth
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 * @see org.dspace.app.rest.ShibbolethRestController
 * @see org.dspace.authenticate.ShibAuthentication
 */
public class ShibbolethAuthenticationFilter extends StatelessLoginFilter {

    public ShibbolethAuthenticationFilter(String url, AuthenticationManager authenticationManager,
            RestAuthenticationService restAuthenticationService) {
        super(url, authenticationManager, restAuthenticationService);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        // In the case of Shibboleth, this method does NOT actually authenticate us. The authentication
        // has already happened in Shibboleth & we are just intercepting the return request in order to check
        // for a valid Shibboleth login (using ShibAuthentication.authenticate()) & add user info into our JWT.
        // See org.dspace.app.rest.ShibbolethRestController JavaDocs for an outline of the entire Shib login process.
        return authenticationManager.authenticate(
                new DSpaceAuthentication(null, null, new ArrayList<>())
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {
        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        // OVERRIDE DEFAULT behavior of StatelessLoginFilter to return a temporary, single use cookie containing
        // the Auth Token (JWT). This Cookie is required because ShibbolethRestController *redirects* the user
        // back to the client/UI after a successful Shibboleth login. Headers cannot be sent via a redirect, so a Cookie
        // must be sent to provide the auth token to the client. On the next request from the client, the cookie is
        // read and destroyed & the Auth token is only used in the Header from that point forward.
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, true);
        chain.doFilter(req, res);
    }

}
