/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.dspace.authorize.AuthorizeException;

/**
 * REST Authorization exception wrapper of the DSpace API AuthorizeException to make it unchecked
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class RESTAuthorizationException extends RuntimeException {

    public RESTAuthorizationException(String message) {
        super(message);
    }

    public RESTAuthorizationException(AuthorizeException ae) {
        super(ae);
    }

}
