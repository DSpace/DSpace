/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

/**
 * Raised when the OAuth2 configuration is missing, invalid or unsupported.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class OAuth2ConfigurationException extends OAuth2Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Create an exception with a message.
     *
     * @param message the detail message
     */
    public OAuth2ConfigurationException(String message) {
        super(message);
    }

    /**
     * Create an exception with a message and a cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public OAuth2ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
