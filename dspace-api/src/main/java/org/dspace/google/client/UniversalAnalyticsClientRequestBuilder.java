/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static org.apache.commons.lang.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.google.GoogleAnalyticsEvent;

/**
 * Implementation of {@link GoogleAnalyticsClientRequestBuilder} that compose
 * the request for Universal Analytics (UA).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
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
    public List<String> composeRequestsBody(String analyticsKey, List<GoogleAnalyticsEvent> events) {

        if (!startsWith(analyticsKey, "UA-")) {
            throw new IllegalArgumentException("Only keys with UA- prefix are supported");
        }

        String requestBody = events.stream()
            .map(event -> formatEvent(analyticsKey, event))
            .collect(Collectors.joining("\n"));

        return isNotEmpty(requestBody) ? List.of(requestBody) : List.of();
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
