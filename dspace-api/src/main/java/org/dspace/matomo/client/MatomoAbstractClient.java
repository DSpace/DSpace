/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.matomo.exception.MatomoClientException;
import org.dspace.matomo.model.MatomoCookieConverter;
import org.dspace.matomo.model.MatomoRequestDetails;

/**
 *
 * {@code abstract} client for Matomo integration
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public abstract class MatomoAbstractClient<C, T, U> implements MatomoClient {

    protected final String baseUrl;
    protected final String token;
    protected final MatomoRequestBuilder matomoRequestBuilder;
    protected final MatomoResponseReader matomoResponseReader;
    protected final C httpClient;
    protected final Logger log = LogManager.getLogger(getClass());

    public MatomoAbstractClient(
        String baseUrl, String token,
        MatomoRequestBuilder matomoRequestBuilder, MatomoResponseReader matomoResponseReader,
        C httpClient
    ) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.matomoRequestBuilder = matomoRequestBuilder;
        this.matomoResponseReader = matomoResponseReader;
        this.httpClient = httpClient;
    }

    protected T createRequest(String requestBody) {
        return createRequest(requestBody, "");
    }

    protected abstract T createRequest(String requestBody, String cookies);

    protected abstract void executeRequest(String requestBody, String cookies, BiConsumer<U, String> responseConsumer);

    protected abstract int getStatusCode(U response);

    protected abstract String getResponseContent(U response);

    @Override
    public void sendDetails(MatomoRequestDetails... details) {
        this.sendDetails(Arrays.asList(details));
    }

    protected String createRequestBody(List<MatomoRequestDetails> details) {
        return this.matomoRequestBuilder.buildJSON(new MatomoBulkRequest(token, details));
    }

    public void sendDetails(List<MatomoRequestDetails> details) {
        if (details == null || details.isEmpty()) {
            log.warn("Cannot send an empty request!");
            return;
        }

        if (StringUtils.isEmpty(baseUrl)) {
            log.error("Cannot send these details {} to Matomo - No endpoint configured!", details);
            return;
        }

        try {
            this.executeRequest(
                createRequestBody(details),
                generateCookies(details),
                this::logError
            );
        } catch (Exception ex) {
            throw new MatomoClientException("An error occurs sending events to " + baseUrl, ex);
        }
    }

    protected String generateCookies(List<MatomoRequestDetails> details) {
        return MatomoCookieConverter.convert(details);
    }

    protected static void addCookies(
        HttpURLConnection connection, Map<String, String> cookies
    ) {
        StringBuilder cookiesValue = new StringBuilder();
        if (cookies != null) {
            for (Iterator<Map.Entry<String, String>> iterator = cookies.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, String> entry = iterator.next();
                cookiesValue.append(entry.getKey()).append("=").append(entry.getValue());
                if (iterator.hasNext()) {
                    cookiesValue.append("; ");
                }
            }
        }
        if (!cookiesValue.isEmpty()) {
            connection.setRequestProperty("Cookie", cookiesValue.toString());
        }
    }

    protected void logError(U response, String requestBody) {
        if (isNotSuccessful(response)) {
            String responseMessage = formatErrorMessage(response);
            log.error(
                "Cannot register the event on Matomo, REQUEST: {} - RESPONSE: {}",
                requestBody,
                responseMessage
            );
            throw new MatomoClientException(responseMessage);
        }
        String responseBody = getResponseContent(response);
        MatomoResponse matomoResponse = matomoResponseReader.fromJSON(responseBody);
        if (
                matomoResponse == null ||
                !MatomoResponse.SUCCESS.equals(matomoResponse.status()) ||
                matomoResponse.invalid() > 0
        ) {
            log.error("Unable to track requestBody: {}, response was: {}", requestBody, responseBody);
        }
    }

    protected boolean isNotSuccessful(U response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    protected String formatErrorMessage(U response) {
        return "Status " + getStatusCode(response) + ". Content: " + getResponseContent(response);
    }

}
