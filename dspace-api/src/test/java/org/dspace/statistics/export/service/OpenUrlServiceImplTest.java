/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.dspace.core.Context;
import org.dspace.statistics.export.OpenURLTracker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for the OpenUrlServiceImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenUrlServiceImplTest {

    /**
     * NOTE: Initialized as a Mockito spy in {@link #setUp()}.
     */
    private OpenUrlServiceImpl openUrlService;

    @Mock
    private FailedOpenURLTrackerService failedOpenURLTrackerService;

    @Mock
    private HttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        // spy on the class under test
        openUrlService = Mockito.spy(OpenUrlServiceImpl.class);

        // manually hook up dependencies (@autowire doesn't work when creating instances using Mockito)
        openUrlService.failedOpenUrlTrackerService = failedOpenURLTrackerService;

        // IMPORTANT: mock http client to prevent making REAL http requests
        doReturn(httpClient).when(openUrlService).getHttpClient(any());
    }

    /**
     * Create a mock http response with the given status code.
     * @param statusCode the http status code to use in the mock.
     * @return a mocked http response.
     */
    protected HttpResponse createMockHttpResponse(int statusCode) {
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        return httpResponse;
    }

    /**
     * Create a mock open url tracker with the given url.
     * @param url the url to use in the mock.
     * @return a mocked open url tracker.
     */
    protected OpenURLTracker createMockTracker(String url) {
        OpenURLTracker tracker = mock(OpenURLTracker.class);
        when(tracker.getUrl()).thenReturn(url);

        return tracker;
    }

    /**
     * Test the processUrl method
     */
    @Test
    public void testProcessUrl() throws IOException, SQLException {
        Context context = mock(Context.class);

        doReturn(createMockHttpResponse(HttpURLConnection.HTTP_OK)).when(httpClient).execute(any());
        openUrlService.processUrl(context, "test-url");

        verify(openUrlService, times(0)).logfailed(context, "test-url");
    }

    /**
     * Test the processUrl method when the url connection fails
     */
    @Test
    public void testProcessUrlOnFail() throws IOException, SQLException {
        Context context = mock(Context.class);

        doReturn(createMockHttpResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)).when(httpClient).execute(any());
        doNothing().when(openUrlService).logfailed(any(Context.class), anyString());

        openUrlService.processUrl(context, "test-url");

        verify(openUrlService, times(1)).logfailed(context, "test-url");
    }

    /**
     * Test the ReprocessFailedQueue method
     */
    @Test
    public void testReprocessFailedQueue() throws IOException, SQLException {
        Context context = mock(Context.class);

        List<OpenURLTracker> trackers = List.of(
            createMockTracker("tacker1"),
            createMockTracker("tacker2"),
            createMockTracker("tacker3")
        );

        when(failedOpenURLTrackerService.findAll(any(Context.class))).thenReturn(trackers);

        // NOTE: first http request will return status code 500, next one 404, then 200
        doReturn(
            createMockHttpResponse(HttpURLConnection.HTTP_INTERNAL_ERROR),
            createMockHttpResponse(HttpURLConnection.HTTP_NOT_FOUND),
            createMockHttpResponse(HttpURLConnection.HTTP_OK)
        ).when(httpClient).execute(any());

        openUrlService.reprocessFailedQueue(context);

        verify(openUrlService, times(3)).tryReprocessFailed(any(Context.class), any(OpenURLTracker.class));

        // NOTE: http request for tracker 1 and 2 failed, so tracker 1 and 2 should be kept
        //       http request for tracker 3 succeeded, so tracker 3 should be removed
        verify(failedOpenURLTrackerService, times(0)).remove(any(Context.class), eq(trackers.get(0)));
        verify(failedOpenURLTrackerService, times(0)).remove(any(Context.class), eq(trackers.get(1)));
        verify(failedOpenURLTrackerService, times(1)).remove(any(Context.class), eq(trackers.get(2)));
    }

    /**
     * Test the method that logs the failed urls in the db
     */
    @Test
    public void testLogfailed() throws SQLException {
        Context context = mock(Context.class);
        OpenURLTracker tracker1 = mock(OpenURLTracker.class);

        when(failedOpenURLTrackerService.create(any(Context.class))).thenReturn(tracker1);

        String failedUrl = "failed-url";
        openUrlService.logfailed(context, failedUrl);

        verify(tracker1).setUrl(failedUrl);

        // NOTE: verify that setUploadDate received a timestamp whose value is no less than 5 seconds from now
        ArgumentCaptor<Date> dateArgCaptor = ArgumentCaptor.forClass(Date.class);
        verify(tracker1).setUploadDate(dateArgCaptor.capture());
        assertThat(
            new BigDecimal(dateArgCaptor.getValue().getTime()),
            closeTo(new BigDecimal(new Date().getTime()), new BigDecimal(5000))
        );
    }

    /**
     * Tests whether the timeout gets set to 10 seconds when processing a url
     */
    @Test
    public void testTimeout() throws IOException, SQLException {
        Context context = mock(Context.class);

        // 1. verify processUrl calls getHttpClient and getHttpClientRequestConfig once
        doReturn(createMockHttpResponse(HttpURLConnection.HTTP_OK)).when(httpClient).execute(any());
        openUrlService.processUrl(context, "test-url");
        verify(openUrlService).getHttpClient(any());
        verify(openUrlService).getHttpClientRequestConfig();

        // 2. verify that getHttpClientRequestConfig sets the timeout
        assertThat(openUrlService.getHttpClientRequestConfig().getConnectTimeout(), is(10 * 1000));
    }
}
