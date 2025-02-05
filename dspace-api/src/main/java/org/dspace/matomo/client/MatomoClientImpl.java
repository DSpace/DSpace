/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.matomo.exception.MatomoClientException;
import org.dspace.matomo.model.MatomoRequestDetails;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoClientImpl implements MatomoClient {

    private static final Logger log = LogManager.getLogger(MatomoClientImpl.class);
    private final CloseableHttpClient httpclient;
    private final String baseUrl;
    private final String token;
    private final MatomoRequestBuilder matomoRequestBuilder;

    public MatomoClientImpl(
        String baseUrl, String token,
        MatomoRequestBuilder matomoRequestBuilder,
        CloseableHttpClient httpclient
    ) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.matomoRequestBuilder = matomoRequestBuilder;
        this.httpclient = httpclient;
    }

    @Override
    public void sendDetails(MatomoRequestDetails... details) {

        if (details == null || details.length == 0) {
            log.warn("Cannot send empty details request!");
            return;
        }

        this.sendDetails(Arrays.asList(details));
    }

    @Override
    public void sendDetails(List<MatomoRequestDetails> details) {
        if (details == null || details.isEmpty()) {
            log.warn("Cannot send empty details request!");
            return;
        }

        try {

            HttpPost httpPost = new HttpPost(baseUrl);
            httpPost.setEntity(
                new StringEntity(this.matomoRequestBuilder.buildJSON(new MatomoBulkRequest(token, details)))
            );

            try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
                if (isNotSuccessful(response)) {
                    throw new MatomoClientException(formatErrorMessage(response));
                }
            }

        } catch (MatomoClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MatomoClientException("An error occurs sending events to " + baseUrl, ex);
        }

    }

    private boolean isNotSuccessful(HttpResponse response) {
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
            log.error("An error occurs getting the response content", e);
            return "Generic error";
        }
    }
}
