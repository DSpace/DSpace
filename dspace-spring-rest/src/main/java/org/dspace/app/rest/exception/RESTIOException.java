/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import java.io.IOException;

/**
 * Wrapper of the IO exception that makes it unchecked.
 *
 * @author Michael Spalti
 */
public class RESTIOException extends RuntimeException {

    public RESTIOException(String message) {
        super(message);
    }

    public RESTIOException(IOException ex) {
        super(ex);
    }

    public RESTIOException(String message, Throwable ex) {

        super(message, ex);
    }

}
