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
 * Interface for classes used by {@link GoogleAnalyticsClient} to define the url
 * and the body of the requests to be sent to Google Analytics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface GoogleAnalyticsClientRequestBuilder {

    /**
     * Returns the url of the Google Analytics endpoint.
     *
     * @param  analyticsKey the Google Analytics key
     * @return              the endpoint url
     */
    String getEndpointUrl(String analyticsKey);

    /**
     * Returns the body of the requests to be sent to Google Analytics as string,
     * based on the given analytics key and events.
     *
     * @param  analyticsKey the Google Analytics key
     * @param  events       the events to be sent
     * @return              the requests body as string
     */
    List<String> composeRequestsBody(String analyticsKey, List<GoogleAnalyticsEvent> events);
}
