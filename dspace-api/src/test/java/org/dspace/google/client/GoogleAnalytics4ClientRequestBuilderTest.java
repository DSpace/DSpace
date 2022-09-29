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
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.dspace.google.GoogleAnalyticsEvent;
import org.dspace.services.ConfigurationService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link GoogleAnalytics4ClientRequestBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalytics4ClientRequestBuilderTest {

    private GoogleAnalytics4ClientRequestBuilder requestBuilder;

    private ConfigurationService configurationService = mock(ConfigurationService.class);

    @Before
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

        assertThrows("Only keys with G- prefix are supported",
            IllegalArgumentException.class, () -> requestBuilder.getEndpointUrl("UA-12345"));

    }

    @Test
    public void testGetEndpointUrlWithoutApiSecretConfigured() {

        assertThrows("The API secret must be configured to sent GA4 events",
            GoogleAnalyticsClientException.class, () -> requestBuilder.getEndpointUrl("G-12345"));

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

        JSONObject requestBody = new JSONObject(requestsBody.get(0));
        assertThat(requestBody.get("client_id"), is("123"));

        JSONArray eventsArray = requestBody.getJSONArray("events");
        assertThat(eventsArray.length(), is(1));

        assertEventJsonHasAttributes(eventsArray.getJSONObject(0), "item", "download", "bitstream", "192.168.1.25",
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

        JSONObject requestBody = new JSONObject(requestsBody.get(0));
        assertThat(requestBody.get("client_id"), is("123"));

        JSONArray eventsArray = requestBody.getJSONArray("events");
        assertThat(eventsArray.length(), is(2));

        JSONObject eventJson1 = findEventJsonByDocumentTitle(eventsArray, "Test publication");
        JSONObject eventJson2 = findEventJsonByDocumentTitle(eventsArray, "Test publication 2");

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

        JSONObject firstRequestBody = findRequestBodyByClientId(requestsBody, "123");
        assertThat(firstRequestBody.get("client_id"), is("123"));

        JSONArray firstEventsArray = firstRequestBody.getJSONArray("events");
        assertThat(firstEventsArray.length(), is(2));

        JSONObject eventJson1 = findEventJsonByDocumentTitle(firstEventsArray, "Test publication");
        JSONObject eventJson2 = findEventJsonByDocumentTitle(firstEventsArray, "Test publication 2");

        assertThat(eventJson1, notNullValue());
        assertThat(eventJson2, notNullValue());

        assertEventJsonHasAttributes(eventJson1, "item", "download", "bitstream", "192.168.1.25",
            "Chrome", "REF", "/api/documents/123", "Test publication");

        assertEventJsonHasAttributes(eventJson2, "item", "download", "bitstream", "192.168.1.25",
            "Mozilla Firefox", "REF-2", "/api/documents/12345", "Test publication 2");

        JSONObject secondRequestBody = findRequestBodyByClientId(requestsBody, "987");
        assertThat(secondRequestBody.get("client_id"), is("987"));

        JSONArray secondEventsArray = secondRequestBody.getJSONArray("events");
        assertThat(secondEventsArray.length(), is(1));

        assertEventJsonHasAttributes(secondEventsArray.getJSONObject(0), "item", "download", "bitstream",
            "192.168.1.13", "Postman", "", "/api/documents/654", "Test publication 3");

    }

    private void assertEventJsonHasAttributes(JSONObject event, String name, String action, String category,
        String userIp, String userAgent, String documentReferrer, String documentPath, String documentTitle) {

        assertThat(event.get("name"), is(name));
        assertThat(event.getJSONObject("params"), notNullValue());
        assertThat(event.getJSONObject("params").get("action"), is(action));
        assertThat(event.getJSONObject("params").get("category"), is(category));
        assertThat(event.getJSONObject("params").get("document_title"), is(documentTitle));
        assertThat(event.getJSONObject("params").get("user_ip"), is(userIp));
        assertThat(event.getJSONObject("params").get("user_agent"), is(userAgent));
        assertThat(event.getJSONObject("params").get("document_referrer"), is(documentReferrer));
        assertThat(event.getJSONObject("params").get("document_path"), is(documentPath));
        assertThat(event.getJSONObject("params").get("time"), notNullValue());

    }

    private JSONObject findRequestBodyByClientId(List<String> requestsBody, String clientId) {
        for (String requestBody : requestsBody) {
            JSONObject requestBodyJson = new JSONObject(requestBody);
            if (requestBodyJson.get("client_id").equals(clientId)) {
                return requestBodyJson;
            }
        }
        return null;
    }

    private JSONObject findEventJsonByDocumentTitle(JSONArray events, String documentTitle) {

        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.getJSONObject(i);
            assertThat(event.getJSONObject("params"), notNullValue());
            if (event.getJSONObject("params").get("document_title").equals(documentTitle)) {
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
