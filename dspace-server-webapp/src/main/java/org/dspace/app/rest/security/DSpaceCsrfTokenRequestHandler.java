/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * A custom Spring Security CsrfTokenRequestAttributeHandler which uses the Spring Security BREACH protection
 * (provided by XorCsrfTokenRequestAttributeHandler) *only* when the CSRF token is sent as a "_csrf" request parameter.
 * In all other scenarios, the CsrfTokenRequestAttributeHandler is used instead.
 * <P>
 * NOTE: The DSpace UI always sends the CSRF Token as a request header. It does NOT send it as a "_csrf" request
 * paramter. So, this BREACH protection would ONLY be triggered for custom clients (not the DSpace UI).
 * Therefore, if using this custom class becomes problematic, we could revert to using the default
 * CsrfTokenRequestAttributeHandler without any negative impact on the DSpace UI.
 * <P>
 * This code is copied from the example "SpaCsrfTokenRequestHandler" (for single page applications) from the Spring
 * Security docs at
 * https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa-configuration
 */
public final class DSpaceCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        /*
         * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
         * the CsrfToken when it is rendered in the response body.
         * NOTE: This should never occur from the DSpace UI, so it is only applicable for custom clients.
         */
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        /*
         * If the request contains a request header, use CsrfTokenRequestAttributeHandler
         * to resolve the CsrfToken.  This applies to the DSpace UI which always includes
         * the raw CsrfToken in an HTTP Header.
         */
        if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
            return super.resolveCsrfTokenValue(request, csrfToken);
        }
        /*
         * In all other cases (e.g. if the request contains a request parameter), use
         * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
         * when a server-side rendered form includes the _csrf request parameter as a
         * hidden input.
         * NOTE: This should never occur from the DSpace UI, so it is only applicable for custom clients.
         */
        return this.delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}
