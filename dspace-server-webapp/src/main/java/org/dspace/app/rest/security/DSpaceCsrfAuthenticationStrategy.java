/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Custom SessionAuthenticationStrategy to be used alongside DSpaceCsrfTokenRepository.
 * <P>
 * Because DSpace is Stateless, this class only resets the CSRF Token if the client has attempted to use it (either
 * successfully or unsuccessfully). This ensures that the Token is not changed on every request (since we are stateless
 * every request creates a new Authentication object).
 * <P>
 * This is essentially a customization of Spring Security's CsrfAuthenticationStrategy:
 * https://github.com/spring-projects/spring-security/blob/6.2.x/web/src/main/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.java
 */
public class DSpaceCsrfAuthenticationStrategy implements SessionAuthenticationStrategy {

    private final Log logger = LogFactory.getLog(getClass());

    private final CsrfTokenRepository tokenRepository;

    private CsrfTokenRequestHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();

    /**
     * Creates a new instance
     * @param tokenRepository the {@link CsrfTokenRepository} to use
     */
    public DSpaceCsrfAuthenticationStrategy(CsrfTokenRepository tokenRepository) {
        Assert.notNull(tokenRepository, "tokenRepository cannot be null");
        this.tokenRepository = tokenRepository;
    }

    /**
     * Method is copied from {@link CsrfAuthenticationStrategy#setRequestHandler(CsrfTokenRequestHandler)}
     */
    public void setRequestHandler(CsrfTokenRequestHandler requestHandler) {
        Assert.notNull(requestHandler, "requestHandler cannot be null");
        this.requestHandler = requestHandler;
    }

    /**
     * This method is triggered anytime a new Authentication occurs. As DSpace uses Stateless authentication,
     * this method is triggered on _every request_ after an initial login occurs. This is because the Spring Security
     * 'Authentication' object is recreated on every request.
     * <P>
     * Therefore, for DSpace, we've customized this method to ensure a new CSRF Token is NOT generated each time a new
     * Authentication object is created -- as doing so causes the CSRF Token to change with every request. Instead, we
     * check to see if the client also passed a CSRF token via a querystring parameter (i.e. "_csrf"). If so, this means
     * the client has sent the token in a less secure manner & it must then be regenerated.
     * <P>
     * NOTE: We also automatically regenerate CSRF token on login/logout via JWTTokenRestAuthenticationServiceImpl.
     */
    @Override
    public void onAuthentication(Authentication authentication,
                                 HttpServletRequest request, HttpServletResponse response)
        throws SessionAuthenticationException {


        // Check if token returned in server-side cookie
        CsrfToken token = this.tokenRepository.loadToken(request);

        // For DSpace, this will only be null if we are forcing CSRF token regeneration (e.g. on initial login)
        boolean containsToken = token != null;

        if (containsToken) {
            // Check for CSRF token sent as param in request
            boolean containsParameter = StringUtils.hasLength(request.getParameter(token.getParameterName()));

            // If token exists was sent in a parameter, then we need to reset our token
            // (as sending token in a param is insecure)
            if (containsParameter) {
                resetCSRFToken(request, response);
            }
        }
    }

    /**
     * A custom utility method to force Spring Security to reset the CSRF token. This is used by DSpace to reset
     * the token whenever the CSRF token is passed insecurely (as a request param, see onAuthentication() above)
     * or on logout (see JWTTokenRestAuthenticationServiceImpl)
     * @param request current HTTP request
     * @param response current HTTP response
     * @see org.dspace.app.rest.security.jwt.JWTTokenRestAuthenticationServiceImpl
     */
    public void resetCSRFToken(HttpServletRequest request, HttpServletResponse response) {
        // Note: We first set the token to null & then set a new one. This results in 2 cookies sent,
        // the first being empty and the second having the new token.
        // This behavior is borrowed from Spring Security's CsrfAuthenticationStrategy, see
        // https://github.com/spring-projects/spring-security/blob/6.2.x/web/src/main/java/org/springframework/security/web/csrf/CsrfAuthenticationStrategy.java
        this.tokenRepository.saveToken(null, request, response);
        DeferredCsrfToken deferredCsrfToken = this.tokenRepository.loadDeferredToken(request, response);
        this.requestHandler.handle(request, response, deferredCsrfToken::get);
        // This may look odd, but reading the deferred CSRF token will cause Spring Security to send it back
        // in the next request. This ensures our new token is sent back immediately (instead of in a later request)
        deferredCsrfToken.get();
        this.logger.debug("Replaced CSRF Token");
    }

}
