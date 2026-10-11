/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static java.util.List.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.google.GoogleAnalyticsEvent;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link GoogleAnalytics4ClientRequestBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalytics4ClientRequestBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GoogleAnalytics4ClientRequestBuilder requestBuilder;

    private ConfigurationService configurationService = mock(ConfigurationService.class);

    @BeforeEach
    public void setup() {
        requestBuilder = new GoogleAnalytics4ClientRequestBuilder("https://google-analytics/test");
        requestBuilder.setConfigurationService(configurationService);
    }

    @Test
    public void testGetEndpointUrl() {

        when(configurationService.getProperty("google.analytics.api-secret")).thenReturn("abc123");

        String endpointUrl = requestBuilder.getEndpointUrl("G-12345");
        assertThat(endpointUrl, is("https://google-analytics/test?api_secret=abc123&measurement_id=G-12345"));

    }

    @Test
    public void testGetEndpointUrlWithNotSupportedKey() {

        assertThrows(IllegalArgumentException.class,
            () -> requestBuilder.getEndpointUrl("UA-12345"), "Only keys with G- prefix are supported");

    }

    @Test
    public void testGetEndpointUrlWithoutApiSecretConfigured() {

        assertThrows(GoogleAnalyticsClientException.class,
            () -> requestBuilder.getEndpointUrl("G-12345"), "The API secret must be configured to sent GA4 events");

    }

    @Test
    public void testComposeRequestBodiesWithoutEvents() {

        List<String> requestsBody = requestBuilder.composeRequestsBody("G-12345", List.of());
        assertThat(requestsBody, empty());

    }

    @Test
    public void testComposeRequestBodiesWithSingleEvent() {

        GoogleAnalyticsEvent event = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        List<String> requestsBody = requestBuilder.composeRequestsBody("G-12345", List.of(event));
        assertThat(requestsBody, hasSize(1));

        JsonNode requestBody = MAPPER.readTree(requestsBody.get(0));
        assertThat(requestBody.get("client_id").asString(), is("123"));

        JsonNode eventsArray = requestBody.get("events");
        assertThat(eventsArray.size(), is(1));

        assertEventJsonHasAttributes(eventsArray.get(0), "item", "download", "bitstream", "192.168.1.25",
            "Chrome", "REF", "/api/documents/123", "Test publication");

    }

    @Test
    public void testComposeRequestBodiesWithManyEventsWithSameClientId() {

        GoogleAnalyticsEvent event1 = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        GoogleAnalyticsEvent event2 = buildEvent("123", "192.168.1.25", "Mozilla Firefox", "REF-2",
            "/api/documents/12345", "Test publication 2");

        List<String> requestsBody = requestBuilder.composeRequestsBody("G-12345", List.of(event1, event2));
        assertThat(requestsBody, hasSize(1));

        JsonNode requestBody = MAPPER.readTree(requestsBody.get(0));
        assertThat(requestBody.get("client_id").asString(), is("123"));

        JsonNode eventsArray = requestBody.get("events");
        assertThat(eventsArray.size(), is(2));

        JsonNode eventJson1 = findEventJsonByDocumentTitle(eventsArray, "Test publication");
        JsonNode eventJson2 = findEventJsonByDocumentTitle(eventsArray, "Test publication 2");

        assertThat(eventJson1, notNullValue());
        assertThat(eventJson2, notNullValue());

        assertEventJsonHasAttributes(eventJson1, "item", "download", "bitstream", "192.168.1.25",
            "Chrome", "REF", "/api/documents/123", "Test publication");

        assertEventJsonHasAttributes(eventJson2, "item", "download", "bitstream", "192.168.1.25",
            "Mozilla Firefox", "REF-2", "/api/documents/12345", "Test publication 2");

    }

    @Test
    public void testComposeRequestBodiesWithManyEventsWithDifferentClientId() {

        GoogleAnalyticsEvent event1 = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        GoogleAnalyticsEvent event2 = buildEvent("123", "192.168.1.25", "Mozilla Firefox", "REF-2",
            "/api/documents/12345", "Test publication 2");

        GoogleAnalyticsEvent event3 = buildEvent("987", "192.168.1.13", "Postman", null,
            "/api/documents/654", "Test publication 3");

        List<String> requestsBody = requestBuilder.composeRequestsBody("G-12345", of(event1, event2, event3));
        assertThat(requestsBody, hasSize(2));

        JsonNode firstRequestBody = findRequestBodyByClientId(requestsBody, "123");
        assertThat(firstRequestBody.get("client_id").asString(), is("123"));

        JsonNode firstEventsArray = firstRequestBody.get("events");
        assertThat(firstEventsArray.size(), is(2));

        JsonNode eventJson1 = findEventJsonByDocumentTitle(firstEventsArray, "Test publication");
        JsonNode eventJson2 = findEventJsonByDocumentTitle(firstEventsArray, "Test publication 2");

        assertThat(eventJson1, notNullValue());
        assertThat(eventJson2, notNullValue());

        assertEventJsonHasAttributes(eventJson1, "item", "download", "bitstream", "192.168.1.25",
            "Chrome", "REF", "/api/documents/123", "Test publication");

        assertEventJsonHasAttributes(eventJson2, "item", "download", "bitstream", "192.168.1.25",
            "Mozilla Firefox", "REF-2", "/api/documents/12345", "Test publication 2");

        JsonNode secondRequestBody = findRequestBodyByClientId(requestsBody, "987");
        assertThat(secondRequestBody.get("client_id").asString(), is("987"));

        JsonNode secondEventsArray = secondRequestBody.get("events");
        assertThat(secondEventsArray.size(), is(1));

        assertEventJsonHasAttributes(secondEventsArray.get(0), "item", "download", "bitstream",
            "192.168.1.13", "Postman", "", "/api/documents/654", "Test publication 3");

    }

    private void assertEventJsonHasAttributes(JsonNode event, String name, String action, String category,
        String userIp, String userAgent, String documentReferrer, String documentPath, String documentTitle) {

        assertThat(event.get("name").asString(), is(name));
        assertThat(event.get("params"), notNullValue());
        assertThat(event.get("params").get("action").asString(), is(action));
        assertThat(event.get("params").get("category").asString(), is(category));
        assertThat(event.get("params").get("document_title").asString(), is(documentTitle));
        assertThat(event.get("params").get("user_ip").asString(), is(userIp));
        assertThat(event.get("params").get("user_agent").asString(), is(userAgent));
        assertThat(event.get("params").get("document_referrer").asString(), is(documentReferrer));
        assertThat(event.get("params").get("document_path").asString(), is(documentPath));
        assertThat(event.get("params").get("time"), notNullValue());

    }

    private JsonNode findRequestBodyByClientId(List<String> requestsBody, String clientId) {
        for (String requestBody : requestsBody) {
            JsonNode requestBodyJson = MAPPER.readTree(requestBody);
            if (requestBodyJson.get("client_id").asString().equals(clientId)) {
                return requestBodyJson;
            }
        }
        return null;
    }

    private JsonNode findEventJsonByDocumentTitle(JsonNode events, String documentTitle) {

        for (int i = 0; i < events.size(); i++) {
            JsonNode event = events.get(i);
            assertThat(event.get("params"), notNullValue());
            if (event.get("params").get("document_title").asString().equals(documentTitle)) {
                return event;
            }
        }

        return null;
    }

    private GoogleAnalyticsEvent buildEvent(String clientId, String userIp, String userAgent,
        String documentReferrer, String documentPath, String documentTitle) {
        return new GoogleAnalyticsEvent(clientId, userIp, userAgent, documentReferrer, documentPath, documentTitle);
    }
}
