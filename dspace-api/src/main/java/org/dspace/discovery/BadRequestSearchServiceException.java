/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

/**
 * Exception used by discovery when discovery search exceptions occur in validating user parameter
 */
public class BadRequestSearchServiceException extends Exception {

    public BadRequestSearchServiceException() {
    }

    public BadRequestSearchServiceException(String s) {
        super(s);
    }

    public BadRequestSearchServiceException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BadRequestSearchServiceException(Throwable throwable) {
        super(throwable);
    }
    
}
