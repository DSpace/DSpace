/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.google.GoogleAnalyticsEvent;

public class UniversalAnalyticsClientRequestBuilder implements GoogleAnalyticsClientRequestBuilder {

    private final String endpointUrl;

    public UniversalAnalyticsClientRequestBuilder(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String getEndpointUrl(String analyticsKey) {
        return endpointUrl;
    }

    @Override
    public String composeRequestBody(String analyticsKey, List<GoogleAnalyticsEvent> events) {
        return events.stream()
            .map(event -> formatEvent(analyticsKey, event))
            .collect(Collectors.joining("\n"));
    }

    private String formatEvent(String analyticsKey, GoogleAnalyticsEvent event) {
        return "v=1" +
            "&tid=" + analyticsKey +
            "&cid=" + event.getClientId() +
            "&t=event" +
            "&uip=" + encodeParameter(event.getUserIp()) +
            "&ua=" + encodeParameter(event.getUserAgent()) +
            "&dr=" + encodeParameter(event.getDocumentReferrer()) +
            "&dp=" + encodeParameter(event.getDocumentPath()) +
            "&dt=" + encodeParameter(event.getDocumentTitle()) +
            "&qt=" + (System.currentTimeMillis() - event.getTime()) +
            "&ec=bitstream" +
            "&ea=download" +
            "&el=item";
    }

    private String encodeParameter(String parameter) {
        return URLEncoder.encode(parameter, StandardCharsets.UTF_8);
    }

}
