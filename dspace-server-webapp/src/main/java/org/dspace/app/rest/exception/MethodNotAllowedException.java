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
 * Method Not Allowed: The 405 status code indicates that the method
 *    received in the request-line is known by the origin server but not
 *    supported by the target resource.  The origin server MUST generate an
 *    Allow header field in a 405 response containing a list of the target
 *    resource's currently supported methods.
 *
 * @author Maria Verdonck
 */
@ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED, reason = "Method not allowed")
public class MethodNotAllowedException extends RuntimeException {

    public MethodNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MethodNotAllowedException(String message) {
        super(message);
    }

}
