/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * The exception used when crud method is called without a pageable object.
 *
 * @author Michael Spalti
 */
public class MissingPaginationException extends RuntimeException {

    public MissingPaginationException(String message) {
        super(message);
    }

}
