/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google;

import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.dspace.app.matcher.GoogleAnalyticsEventMatcher.event;
import static org.dspace.builder.BitstreamBuilder.createBitstream;
import static org.dspace.google.GoogleAsyncEventListener.GA_MAX_EVENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.google.client.GoogleAnalyticsClient;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link GoogleAsyncEventListener}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAsyncEventListenerIT extends AbstractControllerIntegrationTest {

    private static final String ANALYTICS_KEY = "G-123456";

    @Autowired
    private GoogleAsyncEventListener googleAsyncEventListener;

    @Autowired
    private ConfigurationService configurationService;

    private Bitstream bitstream;

    private Item item;

    private List<GoogleAnalyticsClient> originalGoogleAnalyticsClients;

    private GoogleAnalyticsClient firstGaClientMock = mock(GoogleAnalyticsClient.class);

    private GoogleAnalyticsClient secondGaClientMock = mock(GoogleAnalyticsClient.class);

    @Before
    public void setup() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Test community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Test collection")
            .build();

        item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        bitstream = createBitstream(context, item, toInputStream("Test bitstream", defaultCharset()))
            .withName("bitstream.txt")
            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty("google.analytics.key", ANALYTICS_KEY);

        originalGoogleAnalyticsClients = googleAsyncEventListener.getGoogleAnalyticsClients();
        googleAsyncEventListener.setGoogleAnalyticsClients(List.of(firstGaClientMock, secondGaClientMock));

        when(firstGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(false);
        when(secondGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(true);

    }

    @After
    public void cleanup() {
        googleAsyncEventListener.setGoogleAnalyticsClients(originalGoogleAnalyticsClients);
    }

    @Test
    public void testOnBitstreamContentDownload() throws Exception {

        assertThat(getStoredEventsAsList(), empty());

        String bitstreamUrl = "/api/core/bitstreams/" + bitstream.getID() + "/content";

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadBitstreamContent("Chrome", "ABCDEFG", "REF-1");
        downloadBitstreamContent("Chrome", "987654", "REF-2");

        List<GoogleAnalyticsEvent> storedEvents = getStoredEventsAsList();

        assertThat(storedEvents, contains(
            event("123456", "127.0.0.1", "Postman", "REF", bitstreamUrl, "Test item"),
            event("ABCDEFG", "127.0.0.1", "Chrome", "REF-1", bitstreamUrl, "Test item"),
            event("987654", "127.0.0.1", "Chrome", "REF-2", bitstreamUrl, "Test item")));

        googleAsyncEventListener.sendCollectedEvents();

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).sendEvents(ANALYTICS_KEY, storedEvents);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

    }

    @Test
    public void testOnBitstreamContentDownloadWithoutConfiguredAnalyticsKey() throws Exception {

        configurationService.setProperty("google.analytics.key", null);

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadBitstreamContent("Chrome", "ABCDEFG", "REF-1");
        downloadBitstreamContent("Chrome", "987654", "REF-2");

        assertThat(getStoredEventsAsList(), empty());

        googleAsyncEventListener.sendCollectedEvents();

        assertThat(getStoredEventsAsList(), empty());

        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

    }

    @Test
    public void testOnBitstreamContentDownloadWithNotSupportedAnalyticsKey() throws Exception {

        when(firstGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(false);
        when(secondGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(false);

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadBitstreamContent("Chrome", "ABCDEFG", "REF-1");
        downloadBitstreamContent("Chrome", "987654", "REF-2");

        assertThat(getStoredEventsAsList(), hasSize(3));

        IllegalStateException illegalStateException = Assert.assertThrows(IllegalStateException.class,
            () -> googleAsyncEventListener.sendCollectedEvents());

        assertThat(illegalStateException.getMessage(), is("No Google Analytics Client supports key G-123456"));

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

    }

    @Test
    public void testOnBitstreamContentDownloadWithMoreThanOneSupportedClient() throws Exception {

        when(firstGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(true);
        when(secondGaClientMock.isAnalyticsKeySupported(ANALYTICS_KEY)).thenReturn(true);

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadBitstreamContent("Chrome", "ABCDEFG", "REF-1");
        downloadBitstreamContent("Chrome", "987654", "REF-2");

        assertThat(getStoredEventsAsList(), hasSize(3));

        IllegalStateException exception = Assert.assertThrows(IllegalStateException.class,
            () -> googleAsyncEventListener.sendCollectedEvents());

        assertThat(exception.getMessage(), is("More than one Google Analytics Client supports key G-123456"));

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

    }

    @Test
    public void testOnBitstreamContentDownloadWithTooManyEvents() throws Exception {

        int eventCount = GA_MAX_EVENTS + 6;

        for (int i = 0; i < eventCount; i++) {
            downloadBitstreamContent("Postman", "123456", "REF");
        }

        List<GoogleAnalyticsEvent> storedEvents = getStoredEventsAsList();
        assertThat(storedEvents, hasSize(eventCount));

        googleAsyncEventListener.sendCollectedEvents();

        List<GoogleAnalyticsEvent> firstEventsChunk = storedEvents.subList(0, GA_MAX_EVENTS);
        List<GoogleAnalyticsEvent> secondEventsChunk = storedEvents.subList(GA_MAX_EVENTS, eventCount);

        assertThat(getStoredEventsAsList(), is(secondEventsChunk));

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).sendEvents(ANALYTICS_KEY, firstEventsChunk);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

        googleAsyncEventListener.sendCollectedEvents();

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock, times(2)).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock, times(2)).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).sendEvents(ANALYTICS_KEY, secondEventsChunk);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);

    }

    @Test
    public void testOnBitstreamContentDownloadDefaultBundleConfig() throws Exception {
        context.turnOffAuthorisationSystem();
        Bundle licenseBundle = BundleBuilder.createBundle(context, item)
                                            .withName(Constants.LICENSE_BUNDLE_NAME).build();
        Bitstream license = BitstreamBuilder.createBitstream(context, licenseBundle,
                                                             toInputStream("License", defaultCharset())).build();
        context.restoreAuthSystemState();

        assertThat(getStoredEventsAsList(), empty());

        String bitstreamUrl = "/api/core/bitstreams/" + bitstream.getID() + "/content";

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadContent("Chrome", "ABCDEFG", "REF-1", license);

        assertThat(getStoredEventsAsList(), hasSize(1));

        List<GoogleAnalyticsEvent> storedEvents = getStoredEventsAsList();

        assertThat(storedEvents, contains(
            event("123456", "127.0.0.1", "Postman", "REF", bitstreamUrl, "Test item"))
        );

        googleAsyncEventListener.sendCollectedEvents();

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).sendEvents(ANALYTICS_KEY, storedEvents);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);
    }

    @Test
    public void testOnBitstreamContentDownloadMultipleBundleConfig() throws Exception {
        configurationService.setProperty("google-analytics.bundles",
                                         List.of(Constants.DEFAULT_BUNDLE_NAME, "CONTENT"));

        context.turnOffAuthorisationSystem();
        Bundle contentBundle = BundleBuilder.createBundle(context, item).withName("CONTENT").build();
        Bitstream content = BitstreamBuilder.createBitstream(context, contentBundle,
                                                             toInputStream("Test Content", defaultCharset())).build();
        Bundle thumbnailBundle = BundleBuilder.createBundle(context, item).withName("THUMBNAIL").build();
        Bitstream thumbnail = BitstreamBuilder.createBitstream(context, thumbnailBundle,
                                                               toInputStream("Thumbnail", defaultCharset())).build();
        context.restoreAuthSystemState();

        assertThat(getStoredEventsAsList(), empty());

        String bitstreamUrl = "/api/core/bitstreams/" + bitstream.getID() + "/content";
        String contentUrl = "/api/core/bitstreams/" + content.getID() + "/content";

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadContent("Chrome", "ABCDEFG", "REF-1", content);
        downloadContent("Chrome", "987654", "REF-2", thumbnail);

        assertThat(getStoredEventsAsList(), hasSize(2));

        List<GoogleAnalyticsEvent> storedEvents = getStoredEventsAsList();

        assertThat(storedEvents, contains(
            event("123456", "127.0.0.1", "Postman", "REF", bitstreamUrl, "Test item"),
            event("ABCDEFG", "127.0.0.1", "Chrome", "REF-1", contentUrl, "Test item")
            ));

        googleAsyncEventListener.sendCollectedEvents();

        assertThat(getStoredEventsAsList(), empty());

        verify(firstGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).isAnalyticsKeySupported(ANALYTICS_KEY);
        verify(secondGaClientMock).sendEvents(ANALYTICS_KEY, storedEvents);
        verifyNoMoreInteractions(firstGaClientMock, secondGaClientMock);
    }

    @Test
    public void testOnBitstreamContentDownloadNoneBundleConfig() throws Exception {
        configurationService.setProperty("google-analytics.bundles", "none");

        context.turnOffAuthorisationSystem();
        Bundle contentBundle = BundleBuilder.createBundle(context, item).withName("CONTENT").build();
        Bitstream content = BitstreamBuilder.createBitstream(context, contentBundle,
                                                             toInputStream("Test Content", defaultCharset())).build();
        Bundle thumbnailBundle = BundleBuilder.createBundle(context, item).withName("THUMBNAIL").build();
        Bitstream thumbnail = BitstreamBuilder.createBitstream(context, thumbnailBundle,
                                                               toInputStream("Thumbnail", defaultCharset())).build();
        context.restoreAuthSystemState();

        assertThat(getStoredEventsAsList(), empty());

        downloadBitstreamContent("Postman", "123456", "REF");
        downloadContent("Chrome", "ABCDEFG", "REF-1", content);
        downloadContent("Chrome", "987654", "REF-2", thumbnail);

        assertThat(getStoredEventsAsList(), empty());
    }

    @SuppressWarnings("unchecked")
    private List<GoogleAnalyticsEvent> getStoredEventsAsList() {
        List<GoogleAnalyticsEvent> events = new ArrayList<>();

        googleAsyncEventListener.getEventsBuffer().iterator()
            .forEachRemaining(obj -> events.add((GoogleAnalyticsEvent) obj));

        return events;
    }

    private void downloadContent(String userAgent, String correlationId, String referrer, Bitstream bit)
            throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/core/bitstreams/" + bit.getID() + "/content")
                         .header("USER-AGENT", userAgent)
                         .header("X-CORRELATION-ID", correlationId)
                         .header("X-REFERRER", referrer))
            .andExpect(status().isOk());
    }

    private void downloadBitstreamContent(String userAgent, String correlationId, String referrer) throws Exception {
        downloadContent(userAgent, correlationId, referrer, bitstream);
    }

}
