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
 * Exception thrown when a requested resource has not been modified (HTTP 304).
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@ResponseStatus(value = HttpStatus.NOT_MODIFIED, reason = "Not Modified")
public class NotModifiedException extends RuntimeException {

    /**
     * Constructs a NotModifiedException with the default message.
     */
    public NotModifiedException() {
        super("Not Modified");
    }

    /**
     * Constructs a NotModifiedException with a custom message.
     *
     * @param message the detail message
     */
    public NotModifiedException(String message) {
        super(message);
    }
}
