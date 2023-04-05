/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.dspace.app.rest.utils.RestDiscoverQueryBuilder;

/**
 * This exception is thrown when the given search configuration
 * passed to {@link RestDiscoverQueryBuilder} is invalid
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class InvalidSearchRequestException extends RuntimeException {

    public InvalidSearchRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSearchRequestException(String message) {
        super(message);
    }

}
