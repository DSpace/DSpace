/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Custom SessionAuthenticationStrategy to be used alongside DSpaceCsrfTokenRepository.
 * <P>
 * Because DSpace is Stateless, this class only resets the CSRF Token if the client has attempted to use it (either
 * successfully or unsuccessfully). This ensures that the Token is not changed on every request (since we are stateless
 * every request creates a new Authentication object).
 * <P>
 * Based on Spring Security's CsrfAuthenticationStrategy:
 * https://github.com/spring-projects/spring-security/blob/5.2.x/web/src/main/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.java
 */
public class DSpaceCsrfAuthenticationStrategy implements SessionAuthenticationStrategy {

    private final CsrfTokenRepository csrfTokenRepository;

    /**
     * Creates a new instance
     * @param csrfTokenRepository the {@link CsrfTokenRepository} to use
     */
    public DSpaceCsrfAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        Assert.notNull(csrfTokenRepository, "csrfTokenRepository cannot be null");
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * This method is triggered anytime a new Authentication occurs. As DSpace uses Stateless authentication,
     * this method is triggered on _every request_ after an initial login occurs. This is because the Spring Security
     * Authentication object is recreated on every request.
     * <P>
     * Therefore, for DSpace, we've customized this method to ensure a new CSRF Token is NOT generated each time a new
     * Authentication object is created -- doing so causes the CSRF Token to change with every request. Instead, we
     * check to see if the client also passed a CSRF token via a header or parameter. If so, this means the client
     * has used (or attempted to use) the token & it must then be regenerated.
     */
    @Override
    public void onAuthentication(Authentication authentication,
                                 HttpServletRequest request, HttpServletResponse response)
        throws SessionAuthenticationException {

        // Check if token returned in server-side cookie
        CsrfToken token = this.csrfTokenRepository.loadToken(request);
        // For DSpace, this will only be null if we are forcing CSRF token regeneration (e.g. on initial login)
        boolean containsToken = token != null;

        if (containsToken) {
            // Check for header or parameter in request
            boolean containsHeader = StringUtils.hasLength(request.getHeader(token.getHeaderName()));
            boolean containsParameter = StringUtils.hasLength(request.getParameter(token.getParameterName()));

            // If token exists & we've also been sent either the header or parameter
            // then we need to reset our token (as it's been used)
            if (containsHeader || containsParameter) {
                this.csrfTokenRepository.saveToken(null, request, response);

                CsrfToken newToken = this.csrfTokenRepository.generateToken(request);
                this.csrfTokenRepository.saveToken(newToken, request, response);

                request.setAttribute(CsrfToken.class.getName(), newToken);
                request.setAttribute(newToken.getParameterName(), newToken);
            }
        }
    }

}
