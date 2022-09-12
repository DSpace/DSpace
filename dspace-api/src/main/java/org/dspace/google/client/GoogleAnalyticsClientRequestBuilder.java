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

public interface GoogleAnalyticsClientRequestBuilder {

    String getEndpointUrl(String analyticsKey);

    String composeRequestBody(String analyticsKey, List<GoogleAnalyticsEvent> events);
}
