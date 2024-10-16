/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.exception;

/**
 * This exception should be thrown if an unexpected error was encountered when making the remote HTTP request
 * or in parsing / deserialising the response body.
 * This exception should be caught in ExternalDataProviders when making external lookups / searches and handled
 * appropriately.
 *
 * @author Kim Shepherd
 */
public class ExternalDataRestClientException extends ExternalDataException {

    public ExternalDataRestClientException() {
        super();
    }

    public ExternalDataRestClientException(Throwable throwable) {
        super(throwable);
    }

    public ExternalDataRestClientException(String message) {
        super(message);
    }

}