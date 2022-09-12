/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

public class GoogleAnalyticsClientException extends RuntimeException {

    private static final long serialVersionUID = -2248100136404696572L;

    public GoogleAnalyticsClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public GoogleAnalyticsClientException(String message) {
        super(message);
    }

    public GoogleAnalyticsClientException(Throwable cause) {
        super(cause);
    }

}
