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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * This class will filter login requests to try and authenticate them
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class StatelessLoginFilter extends AbstractAuthenticationProcessingFilter {
    private static final Logger log = LoggerFactory.getLogger(StatelessLoginFilter.class);

    protected AuthenticationManager authenticationManager;

    protected RestAuthenticationService restAuthenticationService;

    @Override
    public void afterPropertiesSet() {
    }

    public StatelessLoginFilter(String url, AuthenticationManager authenticationManager,
                                RestAuthenticationService restAuthenticationService) {
        super(new AntPathRequestMatcher(url));
        this.authenticationManager = authenticationManager;
        this.restAuthenticationService = restAuthenticationService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        return authenticationManager.authenticate(
                new DSpaceAuthentication(user, password, new ArrayList<>())
        );
    }


    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, false);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response, AuthenticationException failed)
            throws IOException, ServletException {

        String authenticateHeaderValue = restAuthenticationService.getWwwAuthenticateHeaderValue(request, response);

        response.setHeader("WWW-Authenticate", authenticateHeaderValue);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed!");
        log.error("Authentication failed (status:{})",
                  HttpServletResponse.SC_UNAUTHORIZED, failed);
    }

}
