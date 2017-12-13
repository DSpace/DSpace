/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This class provides an exception to be used when the SearchFilter given is invalid
 */
public class InvalidSearchFilterException extends InvalidRequestException {
    public InvalidSearchFilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
