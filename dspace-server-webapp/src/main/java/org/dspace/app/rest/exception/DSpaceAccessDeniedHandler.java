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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

/**
 * This Handler customizes behavior of AccessDeniedException errors thrown by Spring Security/Boot
 */
@Component
public class DSpaceAccessDeniedHandler implements AccessDeniedHandler {

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

        // Get access to our general exception handler
        DSpaceApiExceptionControllerAdvice handler = new DSpaceApiExceptionControllerAdvice();

        // If a CSRF Token was passed in but was invalid pass to csrfTokenException()
        if (ex instanceof InvalidCsrfTokenException || ex instanceof MissingCsrfTokenException) {
            handler.csrfTokenException(request, response, ex);
            return;
        }

        // Otherwise, our handleAuthorizeException method will deal with generic AccessDeniedExceptions
        handler.handleAuthorizeException(request, response, ex);
    }
}
