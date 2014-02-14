/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.context;

public class ContextServiceException extends Exception {
    public ContextServiceException() {
    }

    public ContextServiceException(String message) {
        super(message);
    }

    public ContextServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextServiceException(Throwable cause) {
        super(cause);
    }
}
