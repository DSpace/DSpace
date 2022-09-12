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

public interface GoogleAnalyticsClient {

    boolean isAnalyticsKeySupported(String analyticsKey);

    void sendEvents(String analyticsKey, List<GoogleAnalyticsEvent> events);
}
