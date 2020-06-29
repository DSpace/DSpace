/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

/**
 * Thrown when there is a problem with Shibboleth authentication process.
 *
 * @author Paulo Graça
 * @version $Revision$
 */
public class ShibAuthenticationException extends Exception {
    public ShibAuthenticationException() {
        super();
    }

    public ShibAuthenticationException(String message) {
        super(message);
    }

    public ShibAuthenticationException(Throwable cause) {
        super(cause);
    }

    public ShibAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
