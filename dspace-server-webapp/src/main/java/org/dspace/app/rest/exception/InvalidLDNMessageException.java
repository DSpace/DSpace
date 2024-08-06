/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;


/**
 * This exception is thrown when the given LDN Message json is invalid
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class InvalidLDNMessageException extends RuntimeException {

    public InvalidLDNMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLDNMessageException(String message) {
        super(message);
    }

}
