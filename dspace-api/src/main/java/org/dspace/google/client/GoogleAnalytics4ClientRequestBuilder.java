/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang.StringUtils.startsWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.google.GoogleAnalyticsEvent;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link GoogleAnalyticsClientRequestBuilder} that compose
 * the request for Google Analytics 4 (GA4).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalytics4ClientRequestBuilder implements GoogleAnalyticsClientRequestBuilder {

    private final String endpointUrl;

    @Autowired
    private ConfigurationService configurationService;

    private ObjectMapper objectMapper = new ObjectMapper();

    public GoogleAnalytics4ClientRequestBuilder(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String getEndpointUrl(String analyticsKey) {

        if (!startsWith(analyticsKey, "G-")) {
            throw new IllegalArgumentException("Only keys with G- prefix are supported");
        }

        String apiSecret = configurationService.getProperty("google.analytics.api-secret");
        if (StringUtils.isBlank(apiSecret)) {
            throw new GoogleAnalyticsClientException("The API secret must be configured to sent GA4 events");
        }

        return endpointUrl + "?api_secret=" + apiSecret + "&measurement_id=" + analyticsKey;

    }

    @Override
    public List<String> composeRequestsBody(String analyticsKey, List<GoogleAnalyticsEvent> events) {

        Map<String, List<GoogleAnalyticsEvent>> eventsGroupedByClientId = groupByClientId(events);

        List<String> requestsBody = new ArrayList<String>();

        for (String clientId : eventsGroupedByClientId.keySet()) {
            String requestBody = composeRequestBody(clientId, eventsGroupedByClientId.get(clientId));
            requestsBody.add(requestBody);
        }

        return requestsBody;

    }

    private Map<String, List<GoogleAnalyticsEvent>> groupByClientId(List<GoogleAnalyticsEvent> events) {
        return events.stream()
            .collect(groupingBy(GoogleAnalyticsEvent::getClientId));
    }

    private String composeRequestBody(String clientId, List<GoogleAnalyticsEvent> events) {

        GoogleAnalytics4EventsVO eventsVo = new GoogleAnalytics4EventsVO(clientId);

        events.stream()
            .map(GoogleAnalytics4EventVO::fromGoogleAnalyticsEvent)
            .forEach(eventsVo::addEvent);

        return toJsonAsString(eventsVo);

    }

    private String toJsonAsString(GoogleAnalytics4EventsVO eventsVo) {
        try {
            return objectMapper.writeValueAsString(eventsVo);
        } catch (JsonProcessingException e) {
            throw new GoogleAnalyticsClientException(e);
        }
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * Class that models the json of the events to be write in the body of the GA request.
     */
    public static class GoogleAnalytics4EventsVO {

        @JsonProperty("client_id")
        private final String clientId;

        private final List<GoogleAnalytics4EventVO> events;

        public GoogleAnalytics4EventsVO(String clientId) {
            this.clientId = clientId;
            this.events = new ArrayList<>();
        }

        public String getClientId() {
            return clientId;
        }

        public List<GoogleAnalytics4EventVO> getEvents() {
            return events;
        }

        public void addEvent(GoogleAnalytics4EventVO event) {
            this.events.add(event);
        }

    }

    /**
     * Class that model a single event to be sent to GA.
     */
    public static class GoogleAnalytics4EventVO {

        private final String name = "item";

        private final GoogleAnalytics4EventParamsVO params;

        public static GoogleAnalytics4EventVO fromGoogleAnalyticsEvent(GoogleAnalyticsEvent event) {
            return new GoogleAnalytics4EventVO(event.getTime(), event.getDocumentTitle(), event.getDocumentPath(),
                event.getDocumentReferrer(), event.getUserAgent(), event.getUserIp());
        }

        public GoogleAnalytics4EventVO(long time, String documentTitle, String documentPath, String documentReferrer,
            String userAgent, String userIp) {

            this.params = new GoogleAnalytics4EventParamsVO(time, documentTitle, documentPath,
                documentReferrer, userAgent, userIp);
        }

        public String getName() {
            return name;
        }

        public GoogleAnalytics4EventParamsVO getParams() {
            return params;
        }

    }

    /**
     * Class that model the params of a specific event to be sent to GA.
     *
     * @author Luca Giamminonni (luca.giamminonni at 4science.it)
     *
     */
    public static class GoogleAnalytics4EventParamsVO {

        private final String action = "download";

        private final String category = "bitstream";

        @JsonInclude(Include.NON_NULL)
        private final long time;

        @JsonInclude(Include.NON_NULL)
        @JsonProperty("document_title")
        private final String documentTitle;

        @JsonInclude(Include.NON_NULL)
        @JsonProperty("document_path")
        private final String documentPath;

        @JsonInclude(Include.NON_NULL)
        @JsonProperty("document_referrer")
        private final String documentReferrer;

        @JsonInclude(Include.NON_NULL)
        @JsonProperty("user_agent")
        private final String userAgent;

        @JsonInclude(Include.NON_NULL)
        @JsonProperty("user_ip")
        private final String userIp;

        public GoogleAnalytics4EventParamsVO(long time, String documentTitle, String documentPath,
            String documentReferrer, String userAgent, String userIp) {
            this.time = time;
            this.documentTitle = documentTitle;
            this.documentPath = documentPath;
            this.documentReferrer = documentReferrer;
            this.userAgent = userAgent;
            this.userIp = userIp;
        }

        public long getTime() {
            return time;
        }

        public String getDocumentTitle() {
            return documentTitle;
        }

        public String getDocumentPath() {
            return documentPath;
        }

        public String getDocumentReferrer() {
            return documentReferrer;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public String getUserIp() {
            return userIp;
        }

        public String getAction() {
            return action;
        }

        public String getCategory() {
            return category;
        }

    }

}
