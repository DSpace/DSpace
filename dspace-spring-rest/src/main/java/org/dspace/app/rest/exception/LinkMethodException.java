/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * This exception captures information when a method cannot be found
 * in the repository for a resource.
 *
 * @author Michael Spalti
 */
public class LinkMethodException extends RuntimeException {

    public LinkMethodException(String message) {
        super(message);
    }

    public LinkMethodException(String message, Throwable cause) {
        super(message, cause);
    }

}
