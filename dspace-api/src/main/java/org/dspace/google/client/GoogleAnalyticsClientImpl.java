/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.google.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dspace.google.GoogleAnalyticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link GoogleAnalyticsClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class GoogleAnalyticsClientImpl implements GoogleAnalyticsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAnalyticsClientImpl.class);

    private final String keyPrefix;

    private final GoogleAnalyticsClientRequestBuilder requestBuilder;

    private final CloseableHttpClient httpclient;

    public GoogleAnalyticsClientImpl(String keyPrefix, GoogleAnalyticsClientRequestBuilder requestBuilder) {
        this.keyPrefix = keyPrefix;
        this.requestBuilder = requestBuilder;
        this.httpclient = HttpClients.createDefault();
    }

    @Override
    public boolean isAnalyticsKeySupported(String analyticsKey) {
        return StringUtils.startsWith(analyticsKey, keyPrefix);
    }

    @Override
    public void sendEvents(String analyticsKey, List<GoogleAnalyticsEvent> events) {

        if (!isAnalyticsKeySupported(analyticsKey)) {
            throw new IllegalArgumentException("The given analytics key " + analyticsKey
                + " is not supported. A key with prefix " + keyPrefix + " is required");
        }

        String endpointUrl = requestBuilder.getEndpointUrl(analyticsKey);

        requestBuilder.composeRequestsBody(analyticsKey, events)
            .forEach(requestBody -> sendRequest(endpointUrl, requestBody));

    }

    private void sendRequest(String endpointUrl, String requestBody) {

        try {

            HttpPost httpPost = new HttpPost(endpointUrl);
            httpPost.setEntity(new StringEntity(requestBody));

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                if (isNotSuccessfull(response)) {
                    throw new GoogleAnalyticsClientException(formatErrorMessage(response));
                }
            }

        } catch (GoogleAnalyticsClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new GoogleAnalyticsClientException("An error occurs sending events to " + endpointUrl, ex);
        }

    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    private String formatErrorMessage(HttpResponse response) {
        return "Status " + getStatusCode(response) + ". Content: " + getResponseContent(response);
    }

    private String getResponseContent(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            LOGGER.error("An error occurs getting the response content", e);
            return "Generic error";
        }
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public GoogleAnalyticsClientRequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

}
