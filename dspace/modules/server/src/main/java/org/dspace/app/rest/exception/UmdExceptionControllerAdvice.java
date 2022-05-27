package org.dspace.app.rest.exception;

import java.io.IOException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import static org.springframework.web.servlet.DispatcherServlet.EXCEPTION_ATTRIBUTE;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * This Controller advice will handle exceptions thrown by UMD customizations to the
 * DSpace REST API module.
 *
 * @see org.dspace.app.rest.exception.DSpaceApiExceptionControllerAdvice
 */
@ControllerAdvice
public class UmdExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private static final Logger log = LogManager.getLogger(org.dspace.app.rest.exception.UmdExceptionControllerAdvice.class);

    /**
     * Set of HTTP error codes to log as ERROR with full stacktrace.
     */
    private static final Set<Integer> LOG_AS_ERROR = Set.of(422);

    /**
     * Add user-friendly error messages to the response body for selected errors.
     * Since the error messages will be exposed to the API user, the exception classes are expected to implement
     * {@link TranslatableException} such that the error messages can be translated.
     */
    @ExceptionHandler({
        UnitNameNotProvidedException.class,
    })
    protected void handleCustomUnprocessableEntityException(HttpServletRequest request, HttpServletResponse response,
                                                            TranslatableException ex) throws IOException {
        Context context = ContextUtil.obtainContext(request);
        sendErrorResponse(
            request, response, null, ex.getLocalizedMessage(context), HttpStatus.UNPROCESSABLE_ENTITY.value()
        );
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