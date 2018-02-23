/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This class makes an Exception to be used when a certain sorting is invalid
 */
public class InvalidSortingException extends InvalidRequestException {
    public InvalidSortingException(String message) {
        super(message);
    }
}
