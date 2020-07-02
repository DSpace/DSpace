/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.exception;

/**
 * Exception used when an External Provider don't implements the invoked method
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class ExternalProviderMethodNotImplementedException extends RuntimeException {

    private static final long serialVersionUID = 5268699485635863003L;

    public ExternalProviderMethodNotImplementedException() {
        super();
    }

    public ExternalProviderMethodNotImplementedException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ExternalProviderMethodNotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalProviderMethodNotImplementedException(String message) {
        super(message);
    }

    public ExternalProviderMethodNotImplementedException(Throwable cause) {
        super(cause);
    }
}
