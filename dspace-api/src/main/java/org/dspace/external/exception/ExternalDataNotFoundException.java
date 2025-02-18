/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.exception;

/**
 * This exception should be thrown if an external resource is not found, e.g. the remote REST API returns
 * a 404 NOT FOUND error, but the http request was otherwise performed successfully.
 * This exception should be caught in ExternalDataProviders when making external lookups / searches and handled
 * gracefully. It is not an unexpected or fatal error.
 *
 * @author Kim Shepherd
 */
public class ExternalDataNotFoundException extends ExternalDataException {

        public ExternalDataNotFoundException() {
            super();
        }

        public ExternalDataNotFoundException(Throwable throwable) {
            super(throwable);
        }

        public ExternalDataNotFoundException(String message) {
            super(message);
        }

}
