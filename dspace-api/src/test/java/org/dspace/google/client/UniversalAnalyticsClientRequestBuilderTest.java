/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import static java.util.List.of;
import static org.apache.commons.lang.StringUtils.countMatches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.dspace.google.GoogleAnalyticsEvent;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link UniversalAnalyticsClientRequestBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class UniversalAnalyticsClientRequestBuilderTest {

    private UniversalAnalyticsClientRequestBuilder requestBuilder;

    @Before
    public void setup() {
        requestBuilder = new UniversalAnalyticsClientRequestBuilder("https://google-analytics/test");
    }

    @Test
    public void testGetEndpointUrl() {

        String endpointUrl = requestBuilder.getEndpointUrl("UA-12345");
        assertThat(endpointUrl, is("https://google-analytics/test"));

    }

    @Test
    public void testComposeRequestBodiesWithoutEvents() {

        List<String> requestsBody = requestBuilder.composeRequestsBody("UA-12345", List.of());
        assertThat(requestsBody, empty());

    }

    @Test
    public void testComposeRequestBodiesWithNotSupportedKey() {

        GoogleAnalyticsEvent event = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        assertThrows("Only keys with G- prefix are supported",
            IllegalArgumentException.class, () -> requestBuilder.composeRequestsBody("G-12345", List.of(event)));

    }

    @Test
    public void testComposeRequestBodiesWithSingleEvent() {

        GoogleAnalyticsEvent event = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        List<String> requestsBody = requestBuilder.composeRequestsBody("UA-12345", List.of(event));
        assertThat(requestsBody, hasSize(1));

        String requestBody = requestsBody.get(0);
        assertThat(countMatches(requestBody, "&qt="), is(1));

        String requestBodyWithoutTime = removeAllTimeSections(requestBody);

        String expectedRequestBodyWithoutTime = "v=1&tid=UA-12345&cid=123&t=event&uip=192.168.1.25&ua=Chrome&dr=REF"
            + "&dp=%2Fapi%2Fdocuments%2F123&dt=Test+publication&ec=bitstream&ea=download&el=item";

        assertThat(requestBodyWithoutTime, is(expectedRequestBodyWithoutTime));

    }

    @Test
    public void testComposeRequestBodiesWithManyEventsWithSameClientId() {

        GoogleAnalyticsEvent event1 = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        GoogleAnalyticsEvent event2 = buildEvent("123", "192.168.1.25", "Mozilla Firefox", "REF-2",
            "/api/documents/12345", "Test publication 2");

        List<String> requestsBody = requestBuilder.composeRequestsBody("UA-12345", List.of(event1, event2));
        assertThat(requestsBody, hasSize(1));
        String requestBody = requestsBody.get(0);

        assertThat(countMatches(requestBody, "&qt="), is(2));

        String requestBodyWithoutTime = removeAllTimeSections(requestBody);

        String expectedRequestBodyWithoutTime = "v=1&tid=UA-12345&cid=123&t=event&uip=192.168.1.25&ua=Chrome&dr=REF"
            + "&dp=%2Fapi%2Fdocuments%2F123&dt=Test+publication&ec=bitstream&ea=download&el=item\n"
            + "v=1&tid=UA-12345&cid=123&t=event&uip=192.168.1.25&ua=Mozilla+Firefox&dr=REF-2"
            + "&dp=%2Fapi%2Fdocuments%2F12345&dt=Test+publication+2&ec=bitstream&ea=download&el=item";

        assertThat(requestBodyWithoutTime, is(expectedRequestBodyWithoutTime));

    }

    @Test
    public void testComposeRequestBodiesWithManyEventsWithDifferentClientId() {

        GoogleAnalyticsEvent event1 = buildEvent("123", "192.168.1.25", "Chrome", "REF",
            "/api/documents/123", "Test publication");

        GoogleAnalyticsEvent event2 = buildEvent("123", "192.168.1.25", "Mozilla Firefox", "REF-2",
            "/api/documents/12345", "Test publication 2");

        GoogleAnalyticsEvent event3 = buildEvent("987", "192.168.1.13", "Postman", null,
            "/api/documents/654", "Test publication 3");

        List<String> requestsBody = requestBuilder.composeRequestsBody("UA-12345", of(event1, event2, event3));
        assertThat(requestsBody, hasSize(1));
        String requestBody = requestsBody.get(0);

        assertThat(countMatches(requestBody, "&qt="), is(3));

        String requestBodyWithoutTime = removeAllTimeSections(requestBody);

        String expectedRequestBodyWithoutTime = "v=1&tid=UA-12345&cid=123&t=event&uip=192.168.1.25&ua=Chrome&dr=REF"
            + "&dp=%2Fapi%2Fdocuments%2F123&dt=Test+publication&ec=bitstream&ea=download&el=item\n"
            + "v=1&tid=UA-12345&cid=123&t=event&uip=192.168.1.25&ua=Mozilla+Firefox&dr=REF-2"
            + "&dp=%2Fapi%2Fdocuments%2F12345&dt=Test+publication+2&ec=bitstream&ea=download&el=item\n"
            + "v=1&tid=UA-12345&cid=987&t=event&uip=192.168.1.13&ua=Postman&dr="
            + "&dp=%2Fapi%2Fdocuments%2F654&dt=Test+publication+3&ec=bitstream&ea=download&el=item";

        assertThat(requestBodyWithoutTime, is(expectedRequestBodyWithoutTime));

    }

    private String removeAllTimeSections(String requestBody) {
        return requestBody.replaceAll("&qt=\\d+", "");
    }

    private GoogleAnalyticsEvent buildEvent(String clientId, String userIp, String userAgent,
        String documentReferrer, String documentPath, String documentTitle) {
        return new GoogleAnalyticsEvent(clientId, userIp, userAgent, documentReferrer, documentPath, documentTitle);
    }
}
