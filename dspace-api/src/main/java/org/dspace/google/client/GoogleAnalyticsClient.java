/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import java.util.List;

import org.dspace.google.GoogleAnalyticsEvent;

/**
 * Client to send events to Google Analytics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface GoogleAnalyticsClient {

    /**
     * Check if the client supports the given analytics key.
     *
     * @param  analyticsKey the analytics key
     * @return              true if the key is supported, false otherwise
     */
    boolean isAnalyticsKeySupported(String analyticsKey);

    /**
     * Send the given Google Analytics events.
     *
     * @param analyticsKey the analytics key
     * @param events       the events to be sent
     */
    void sendEvents(String analyticsKey, List<GoogleAnalyticsEvent> events);
}
