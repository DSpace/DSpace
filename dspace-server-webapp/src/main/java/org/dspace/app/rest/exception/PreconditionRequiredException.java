/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an ETag If-Match header is required but missing (HTTP 428 Precondition Required).
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@ResponseStatus(value = HttpStatus.PRECONDITION_REQUIRED, reason = "Precondition Required")
public class PreconditionRequiredException extends RuntimeException {

    /**
     * Constructs a PreconditionRequiredException with a detail message.
     *
     * @param message the detail message
     */
    public PreconditionRequiredException(String message) {
        super(message);
    }

    /**
     * Constructs a PreconditionRequiredException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public PreconditionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
