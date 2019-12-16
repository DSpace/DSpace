/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Spring security authentication entry point to return a 401 response for unauthorized requests
 * This class is used in the {@link WebSecurityConfiguration} class.
 */
public class DSpace401AuthenticationEntryPoint implements AuthenticationEntryPoint {

    private RestAuthenticationService restAuthenticationService;

    public DSpace401AuthenticationEntryPoint(RestAuthenticationService restAuthenticationService) {
        this.restAuthenticationService = restAuthenticationService;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        response.setHeader("WWW-Authenticate",
                restAuthenticationService.getWwwAuthenticateHeaderValue(request, response));

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                authException.getMessage());
    }
}
