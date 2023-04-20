/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This exception is used when the search service has not been implemented
 * for this server.
 */
@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED, reason = "Method not implemented")
public class NotImplementedException extends RuntimeException {

    public NotImplementedException(String message) {
        super(message);
    }
}
