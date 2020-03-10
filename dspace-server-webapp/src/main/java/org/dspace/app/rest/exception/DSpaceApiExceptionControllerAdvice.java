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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.support.QueryMethodParameterConversionException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * This Controller advice will handle all exceptions thrown by the DSpace API module
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@ControllerAdvice
public class DSpaceApiExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @ExceptionHandler({AuthorizeException.class, RESTAuthorizationException.class, AccessDeniedException.class})
    protected void handleAuthorizeException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        if (restAuthenticationService.hasAuthenticationData(request)) {
            sendErrorResponse(request, response, ex, ex.getMessage(), HttpServletResponse.SC_FORBIDDEN);
        } else {
            sendErrorResponse(request, response, ex, ex.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected void handleIllegalArgumentException(HttpServletRequest request, HttpServletResponse response,
                                                  Exception ex) throws IOException {
        sendErrorResponse(request, response, ex, ex.getMessage(), HttpServletResponse.SC_BAD_REQUEST);
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
                          "An internal read or write operation failed (IO Exception)",
                          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    protected void methodNotAllowedException(HttpServletRequest request, HttpServletResponse response,
                                                  Exception ex) throws IOException {
        sendErrorResponse(request, response, ex, ex.getMessage(), HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler( {UnprocessableEntityException.class})
    protected void handleUnprocessableEntityException(HttpServletRequest request, HttpServletResponse response,
                                                      Exception ex) throws IOException {

        //422 is not defined in HttpServletResponse.  Its meaning is "Unprocessable Entity".
        //Using the value from HttpStatus.
        sendErrorResponse(request, response, null,
                ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler(QueryMethodParameterConversionException.class)
    protected void ParameterConversionException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        sendErrorResponse(request, response, null,
                          ex.getMessage(),
                          HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingParameterException.class)
    protected void MissingParameterException(HttpServletRequest request, HttpServletResponse response, Exception ex)
        throws IOException {
        // we want the 400 status for missing parameters, see https://jira.lyrasis.org/browse/DS-4428
        sendErrorResponse(request, response, null,
                          ex.getMessage(),
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
        sendErrorResponse(request, response, ex, "An Exception has occured", returnCode);

    }


    private void sendErrorResponse(final HttpServletRequest request, final HttpServletResponse response,
                                   final Exception ex, final String message, final int statusCode) throws IOException {
        //Make sure Spring picks up this exception
        request.setAttribute(EXCEPTION_ATTRIBUTE, ex);

        //Exception properties will be set by org.springframework.boot.web.support.ErrorPageFilter
        response.sendError(statusCode, message);
    }

}
