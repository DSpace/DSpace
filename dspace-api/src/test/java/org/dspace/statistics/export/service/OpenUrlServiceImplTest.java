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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.config.RequestConfig;
import org.dspace.core.Context;
import org.dspace.statistics.export.OpenURLTracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test class for the OpenUrlServiceImpl
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenUrlServiceImplTest {

    @InjectMocks
    @Spy
    private OpenUrlServiceImpl openUrlService;

    @Mock
    private FailedOpenURLTrackerService failedOpenURLTrackerService;

    /**
     * Test the processUrl method
     * @throws IOException
     * @throws SQLException
     */
    @Test
    public void testProcessUrl() throws IOException, SQLException {
        Context context = mock(Context.class);

        doReturn(HttpURLConnection.HTTP_OK).when(openUrlService)
                                           .getResponseCodeFromUrl(anyString());
        openUrlService.processUrl(context, "test-url");

        verify(openUrlService, times(0)).logfailed(context, "test-url");
    }

    /**
     * Test the processUrl method when the url connection fails
     * @throws IOException
     * @throws SQLException
     */
    @Test
    public void testProcessUrlOnFail() throws IOException, SQLException {
        Context context = mock(Context.class);

        doReturn(HttpURLConnection.HTTP_INTERNAL_ERROR).when(openUrlService)
                                                       .getResponseCodeFromUrl(anyString());
        doNothing().when(openUrlService).logfailed(any(Context.class), anyString());

        openUrlService.processUrl(context, "test-url");

        verify(openUrlService, times(1)).logfailed(context, "test-url");

    }

    /**
     * Test the ReprocessFailedQueue method
     * @throws SQLException
     */
    @Test
    public void testReprocessFailedQueue() throws SQLException {
        Context context = mock(Context.class);

        List<OpenURLTracker> trackers = new ArrayList<>();
        OpenURLTracker tracker1 = mock(OpenURLTracker.class);
        OpenURLTracker tracker2 = mock(OpenURLTracker.class);
        OpenURLTracker tracker3 = mock(OpenURLTracker.class);

        trackers.add(tracker1);
        trackers.add(tracker2);
        trackers.add(tracker3);

        when(failedOpenURLTrackerService.findAll(any(Context.class))).thenReturn(trackers);
        doNothing().when(openUrlService).tryReprocessFailed(any(Context.class), any(OpenURLTracker.class));

        openUrlService.reprocessFailedQueue(context);

        verify(openUrlService, times(3)).tryReprocessFailed(any(Context.class), any(OpenURLTracker.class));

    }

    /**
     * Test the method that logs the failed urls in the db
     * @throws SQLException
     */
    @Test
    public void testLogfailed() throws SQLException {
        Context context = mock(Context.class);
        OpenURLTracker tracker1 = mock(OpenURLTracker.class);

        doCallRealMethod().when(tracker1).setUrl(anyString());
        when(tracker1.getUrl()).thenCallRealMethod();

        when(failedOpenURLTrackerService.create(any(Context.class))).thenReturn(tracker1);

        String failedUrl = "failed-url";
        openUrlService.logfailed(context, failedUrl);

        assertThat(tracker1.getUrl(), is(failedUrl));

    }

    /**
     * Tests whether the timeout gets set to 10 seconds when processing a url
     * @throws SQLException
     */
    @Test
    public void testTimeout() throws SQLException {
        Context context = mock(Context.class);
        String URL = "http://bla.com";

        RequestConfig.Builder requestConfig = mock(RequestConfig.Builder.class);
        doReturn(requestConfig).when(openUrlService).getRequestConfigBuilder();
        doReturn(requestConfig).when(requestConfig).setConnectTimeout(10 * 1000);
        doReturn(RequestConfig.custom().build()).when(requestConfig).build();

        openUrlService.processUrl(context, URL);

        Mockito.verify(requestConfig).setConnectTimeout(10 * 1000);
    }
}
