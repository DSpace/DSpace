/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * This Handler customizes behavior of AccessDeniedException errors thrown by Spring Security/Boot.
 * <P>
 * More specifically, we use this Handler to ensure exceptions related to CSRF Tokens are also sent to our
 * DSpaceApiExceptionControllerAdvice class, which manages all exceptions for the DSpace backend. Without this
 * handler, those CSRF exceptions are managed by Spring Security/Boot *before* DSpaceApiExceptionControllerAdvice
 * is triggered.
 * <P>
 * Additionally, this Handler is customized to refresh the CSRF Token whenever an InvalidCsrfTokenException occurs.
 * This helps ensure our DSpace server-side token (stored in a server-side cookie) remains "synced" with the token
 * on the client side. If they ever get out of sync, the next request will throw an InvalidCsrfTokenException.
 *
 * @see DSpaceApiExceptionControllerAdvice
 */
@Component
public class DSpaceAccessDeniedHandler implements AccessDeniedHandler {

    @Lazy
    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    /**
     * Override handle() to pass these exceptions over to our DSpaceApiExceptionControllerAdvice handler
     * @param request request
     * @param response response
     * @param ex AccessDeniedException
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
        throws IOException, ServletException {

        // Do nothing if response is already committed
        if (response.isCommitted()) {
            return;
        }

        // If we had an InvalidCsrfTokenException, this means the client sent a CSRF token which did *not* match the
        // token on the server. In this scenario, we trigger a refresh of the CSRF token...as it's possible the user
        // switched clients (from HAL Browser to UI or visa versa) and has an out-of-sync token.
        // NOTE: this logic is tested in AuthenticationRestControllerIT.testRefreshTokenWithInvalidCSRF()
        if (ex instanceof InvalidCsrfTokenException) {
            // Remove current token & generate a new one
            csrfTokenRepository.saveToken(null, request, response);
            CsrfToken newToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(newToken, request, response);
        }

        // Pass the exception to our general exception handler for processing
        // (This results in passing the exception to DSpaceApiExceptionControllerAdvice to handle the response)
        handlerExceptionResolver.resolveException(request, response, null, ex);
    }
}
