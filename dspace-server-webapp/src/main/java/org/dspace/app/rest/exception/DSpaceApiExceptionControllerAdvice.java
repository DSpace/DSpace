/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import static org.springframework.web.servlet.DispatcherServlet.EXCEPTION_ATTRIBUTE;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.support.QueryMethodParameterConversionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * This Controller advice will handle default exceptions thrown by the DSpace REST API module.
 * <P>
 * Keep in mind some specialized handlers exist for specific message types, e.g. DSpaceAccessDeniedHandler
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 * @see DSpaceAccessDeniedHandler
 */
@ControllerAdvice
public class DSpaceApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger log = LogManager.getLogger(DSpaceApiExceptionControllerAdvice.class);

    /**
     * Set of HTTP error codes to log as ERROR with full stacktrace.
     */
    private static final Set<Integer> LOG_AS_ERROR = Set.of(422);

    @ExceptionHandler({AuthorizeException.class, RESTAuthorizationException.class, AccessDeniedException.class})
    protected void handleAuthorizeException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        Context context = ContextUtil.obtainContext(request);
        if (Objects.nonNull(context.getCurrentUser())) {
            sendErrorResponse(request, response, ex, "Access is denied", HttpServletResponse.SC_FORBIDDEN);
        } else {
            sendErrorResponse(request, response, ex, "Authentication is required", HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    // NOTE: DSpaceAccessDeniedHandler does some preprocessing of InvalidCsrfTokenException errors (to reset the
    // CSRF token) before sending error handling to this method.
    @ExceptionHandler({InvalidCsrfTokenException.class, MissingCsrfTokenException.class})
    protected void csrfTokenException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        sendErrorResponse(request, response, ex, "Access is denied. Invalid CSRF token.",
                          HttpServletResponse.SC_FORBIDDEN);
    }

    @ExceptionHandler({IllegalArgumentException.class, MultipartException.class})
    protected void handleWrongRequestException(HttpServletRequest request, HttpServletResponse response,
                                                  Exception ex) throws IOException {
        sendErrorResponse(request, response, ex, "Request is invalid or incorrect", HttpServletResponse.SC_BAD_REQUEST);
    }

    @ExceptionHandler(SQLException.class)
    protected void handleSQLException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        sendErrorResponse(request, response, ex,
                          "An internal database error occurred", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IOException.class)
    protected void handleIOException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        sendErrorResponse(request, response, ex,
                          "An internal read or write operation failed",
                          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    protected void methodNotAllowedException(HttpServletRequest request, HttpServletResponse response,
                                                  Exception ex) throws IOException {
        sendErrorResponse(request, response, ex, "Method is not allowed or supported",
                          HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler( {UnprocessableEntityException.class})
    protected void handleUnprocessableEntityException(HttpServletRequest request, HttpServletResponse response,
                                                      Exception ex) throws IOException {
        //422 is not defined in HttpServletResponse.  Its meaning is "Unprocessable Entity".
        //Using the value from HttpStatus.
        sendErrorResponse(request, response, null,
                "Unprocessable or invalid entity",
                HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler( {InvalidSearchRequestException.class})
    protected void handleInvalidSearchRequestException(HttpServletRequest request, HttpServletResponse response,
                                                      Exception ex) throws IOException {
        sendErrorResponse(request, response, null,
                "Invalid search request",
                HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    /**
     * Add user-friendly error messages to the response body for selected errors.
     * Since the error messages will be exposed to the API user, the exception classes are expected to implement
     * {@link TranslatableException} such that the error messages can be translated.
     */
    @ExceptionHandler({
        RESTEmptyWorkflowGroupException.class,
        EPersonNameNotProvidedException.class,
        GroupNameNotProvidedException.class,
    })
    protected void handleCustomUnprocessableEntityException(HttpServletRequest request, HttpServletResponse response,
                                                            TranslatableException ex) throws IOException {
        Context context = ContextUtil.obtainContext(request);
        sendErrorResponse(
            request, response, null, ex.getLocalizedMessage(context), HttpStatus.UNPROCESSABLE_ENTITY.value()
        );
    }

    @ExceptionHandler(QueryMethodParameterConversionException.class)
    protected void ParameterConversionException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        sendErrorResponse(request, response, null,
                          "A required parameter is invalid",
                          HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingParameterException.class)
    protected void MissingParameterException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        sendErrorResponse(request, response, null,
                          "A required parameter is missing",
                          HttpStatus.BAD_REQUEST.value());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        return super.handleMissingServletRequestParameter(ex, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        return super.handleTypeMismatch(ex, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    protected void handleGenericException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        ResponseStatus responseStatusAnnotation = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);

        int returnCode = 0;
        if (responseStatusAnnotation != null) {
            returnCode = responseStatusAnnotation.code().value();
        } else {
            returnCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        sendErrorResponse(request, response, ex, "An exception has occurred", returnCode);

    }

    /**
     * Send the error to the response.
     * 5xx errors will be logged as ERROR with a full stack trace, 4xx errors will be logged as WARN without a
     * stacktrace. Specific 4xx errors where an ERROR log with full stacktrace is more appropriate are configured in
     * {@link #LOG_AS_ERROR}
     * @param request current request
     * @param response current response
     * @param ex Exception thrown
     * @param message message to log or send in response
     * @param statusCode status code to send in response
     * @throws IOException
     */
    private void sendErrorResponse(final HttpServletRequest request, final HttpServletResponse response,
                                   final Exception ex, final String message, final int statusCode) throws IOException {
        //Make sure Spring picks up this exception
        request.setAttribute(EXCEPTION_ATTRIBUTE, ex);

        // We don't want to fill logs with bad/invalid REST API requests.
        if (HttpStatus.valueOf(statusCode).is5xxServerError() || LOG_AS_ERROR.contains(statusCode)) {
            // Log the full error and status code
            log.error("{} (status:{})", message, statusCode, ex);
        } else if (HttpStatus.valueOf(statusCode).is4xxClientError()) {
            // Log the error as a single-line WARN
            log.warn("{} (status:{})", message, statusCode);
        }

        //Exception properties will be set by org.springframework.boot.web.support.ErrorPageFilter
        response.sendError(statusCode, message);
    }

}
