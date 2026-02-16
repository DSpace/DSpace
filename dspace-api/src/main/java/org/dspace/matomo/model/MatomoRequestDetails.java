/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the details of a single request
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoRequestDetails {

    final Map<String, String> parameters = new HashMap<>();
    final Map<String, String> cookies = new HashMap<>();

    /**
     * Adds a parameter key-value pair to the request details
     *
     * @param key   The parameter key
     * @param value The parameter value
     * @return The current MatomoRequestDetails instance for method chaining
     */
    public MatomoRequestDetails addParameter(String key, String value) {
        parameters.put(key, value);
        return this;
    }

    /**
     * Adds a cookie key-value pair to the request details
     * @param key The cookie key
     * @param value The cookie value
     * @return The current MatomoRequestDetails instance for method chaining
     */
    public MatomoRequestDetails addCookie(String key, String value) {
        cookies.put(key, value);
        return this;
    }

}
