/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.exception;

/**
 * This class represents an Exception that will be used to encapsulate details coming from {@code Matomo}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoClientException extends RuntimeException {

    public MatomoClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MatomoClientException(String message) {
        super(message);
    }

    public MatomoClientException(Throwable cause) {
        super(cause);
    }
}
