/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * This class will filter /api/authn/login requests to try and authenticate them. Keep in mind, this filter runs *after*
 * {@link StatelessAuthenticationFilter} (which looks for authentication data in the request itself). So, in some scenarios
 * (e.g. after a Shibboleth login) the StatelessAuthenticationFilter does the actual authentication, and this Filter
 * just ensures the auth token (JWT) is sent back in an Authorization header.
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class StatelessLoginFilter extends AbstractAuthenticationProcessingFilter {
    private static final Logger log = LogManager.getLogger();

    protected AuthenticationManager authenticationManager;

    protected RestAuthenticationService restAuthenticationService;

    @Override
    public void afterPropertiesSet() {
    }

    /**
     * Initialize a StatelessLoginFilter for the given URL and HTTP method. This login filter will ONLY attempt
     * authentication for requests that match this URL and method. The URL & method are defined in the configuration
     * in WebSecurityConfiguration.
     * @see org.dspace.app.rest.security.WebSecurityConfiguration
     * @param url URL path to attempt to authenticate (e.g. "/api/authn/login")
     * @param httpMethod HTTP method to attempt to authentication (e.g. "POST")
     * @param authenticationManager Spring Security AuthenticationManager to use for authentication
     * @param restAuthenticationService DSpace RestAuthenticationService to use for authentication
     */
    public StatelessLoginFilter(String url, String httpMethod, AuthenticationManager authenticationManager,
                                RestAuthenticationService restAuthenticationService) {
        // NOTE: attemptAuthentication() below will only be triggered by requests that match both this URL and method
        super(new AntPathRequestMatcher(url, httpMethod));
        this.authenticationManager = authenticationManager;
        this.restAuthenticationService = restAuthenticationService;
    }

    /**
     * Attempt to authenticate the user by using Spring Security's AuthenticationManager.
     * The AuthenticationManager will delegate this task to one or more AuthenticationProvider classes.
     * <P>
     * For DSpace, our custom AuthenticationProvider is {@link EPersonRestAuthenticationProvider}, so that
     * is the authenticate() method which is called below.
     *
     * @param req current request
     * @param res current response
     * @return a valid Spring Security Authentication object if authentication succeeds
     * @throws AuthenticationException if authentication fails
     * @see EPersonRestAuthenticationProvider
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {

        String user = req.getParameter("user");
        String password = req.getParameter("password");

        // Attempt to authenticate by passing user & password (if provided) to AuthenticationProvider class(es)
        // NOTE: This method will check if the user was already authenticated by StatelessAuthenticationFilter,
        // and, if so, just refresh their token.
        return authenticationManager.authenticate(new DSpaceAuthentication(user, password));
    }

    /**
     * If the above attemptAuthentication() call was successful (no authentication error was thrown),
     * then this method will take the returned {@link DSpaceAuthentication} class (which includes all
     * the data from the authenticated user) and add the authentication data to the response.
     * <P>
     * For DSpace, this is calling our {@link org.dspace.app.rest.security.jwt.JWTTokenRestAuthenticationServiceImpl}
     * in order to create a JWT based on the authentication data & send that JWT back in the response.
     *
     * @param req current request
     * @param res response
     * @param chain FilterChain
     * @param auth Authentication object containing info about user who had a successful authentication
     * @throws IOException
     * @throws ServletException
     * @see org.dspace.app.rest.security.jwt.JWTTokenRestAuthenticationServiceImpl
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        DSpaceAuthentication dSpaceAuthentication = (DSpaceAuthentication) auth;
        log.debug("Authentication successful for EPerson {}", dSpaceAuthentication::getName);
        restAuthenticationService.addAuthenticationDataForUser(req, res, dSpaceAuthentication, false);
    }

    /**
     * If the above attemptAuthentication() call was unsuccessful, then ensure that the response is a 401 Unauthorized
     * AND it includes a WWW-Authentication header. We use this header in DSpace to return all the enabled
     * authentication options available to the UI (along with the path to the login URL for each option)
     * @param request current request
     * @param response current response
     * @param failed exception that was thrown by attemptAuthentication()
     * @throws IOException
     * @throws ServletException
     */
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
