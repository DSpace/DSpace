/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

/**
 * Wrapper of the dspace BrowseException that makes it unchecked.
 *
 * @author Michael Spalti
 */
public class RESTBrowseException extends RuntimeException {

    public RESTBrowseException(String message) {
        super(message);
    }

    public RESTBrowseException(String message, Throwable io) {
        super(message, io);
    }

}
