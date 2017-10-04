/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * Created by tom on 19/09/2017.
 */
public class InvalidSortingException extends InvalidRequestException {
    public InvalidSortingException(String message) {
        super(message);
    }
}
