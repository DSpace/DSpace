/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.export.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.statistics.export.OpenURLTracker;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the OpenUrlService interface
 */
public class OpenUrlServiceImpl implements OpenUrlService {

    private final Logger log = LogManager.getLogger();

    @Autowired
    protected FailedOpenURLTrackerService failedOpenUrlTrackerService;

    /**
     * Processes the url
     * When the contacting the url fails, the url will be logged in a db table
     * @param c - the context
     * @param urlStr - the url to be processed
     * @throws SQLException
     */
    @Override
    public void processUrl(Context c, String urlStr) throws SQLException {
        log.debug("Prepared to send url to tracker URL: " + urlStr);

        try {
            int responseCode = getResponseCodeFromUrl(urlStr);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logfailed(c, urlStr);
            } else if (log.isDebugEnabled()) {
                log.debug("Successfully posted " + urlStr + " on " + new Date());
            }
        } catch (Exception e) {
            log.error("Failed to send url to tracker URL: " + urlStr);
            logfailed(c, urlStr);
        }
    }

    /**
     * Returns the response code from accessing the url. Returns a http status 408 when the external service doesn't
     * reply in 10 seconds
     *
     * @param urlStr
     * @return response code from the url
     * @throws IOException
     */
    protected int getResponseCodeFromUrl(final String urlStr) throws IOException {
        HttpGet httpGet = new HttpGet(urlStr);
        RequestConfig requestConfig = getRequestConfigBuilder().setConnectTimeout(10 * 1000).build();
        HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        HttpResponse httpResponse = httpClient.execute(httpGet);
        return httpResponse.getStatusLine().getStatusCode();
    }

    protected RequestConfig.Builder getRequestConfigBuilder() {
        return RequestConfig.custom();
    }

    /**
     * Retry to send a failed url
     * @param context
     * @param tracker - db object containing the failed url
     * @throws SQLException
     */
    protected void tryReprocessFailed(Context context, OpenURLTracker tracker) throws SQLException {
        boolean success = false;
        try {

            int responseCode = getResponseCodeFromUrl(tracker.getUrl());

            if (responseCode == HttpURLConnection.HTTP_OK) {
                success = true;
            }
        } catch (Exception e) {
            success = false;
        } finally {
            if (success) {
                failedOpenUrlTrackerService
                        .remove(context, tracker);
                // If the tracker was able to post successfully, we remove it from the database
                log.info("Successfully posted " + tracker.getUrl() + " from " + tracker.getUploadDate());
            } else {
                // Still no luck - write an error msg but keep the entry in the table for future executions
                log.error("Failed attempt from " + tracker.getUrl() + " originating from " + tracker.getUploadDate());
            }
        }
    }

    /**
     * Reprocess all url trackers present in the database
     * @param context
     * @throws SQLException
     */
    @Override
    public void reprocessFailedQueue(Context context) throws SQLException {
        if (failedOpenUrlTrackerService == null) {
            log.error("Error retrieving the \"failedOpenUrlTrackerService\" instance, aborting the processing");
            return;
        }
        List<OpenURLTracker> openURLTrackers = failedOpenUrlTrackerService.findAll(context);
        for (OpenURLTracker openURLTracker : openURLTrackers) {
            tryReprocessFailed(context, openURLTracker);
        }
    }

    /**
     * Log a failed url in the database
     * @param context
     * @param url
     * @throws SQLException
     */
    @Override
    public void logfailed(Context context, String url) throws SQLException {
        Date now = new Date();
        if (StringUtils.isBlank(url)) {
            return;
        }

        OpenURLTracker tracker = failedOpenUrlTrackerService.create(context);
        tracker.setUploadDate(now);
        tracker.setUrl(url);
    }


}
