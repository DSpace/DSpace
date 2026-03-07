/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import java.io.Serial;

/**
 * Exception thrown by {@link GoogleAnalyticsClient} during the events sending.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalyticsClientException extends RuntimeException {

    @Serial
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
