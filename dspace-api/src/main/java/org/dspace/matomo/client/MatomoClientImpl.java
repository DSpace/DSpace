/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.BiConsumer;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.matomo.exception.MatomoClientException;

/**
 * Simple synchronous client for Matomo that uses an {@code CloseableHttpClient} to send out
 * requests.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoClientImpl extends MatomoAbstractClient<CloseableHttpClient, HttpPost, HttpResponse> {

    public MatomoClientImpl(
        String baseUrl, String token,
        MatomoRequestBuilder matomoRequestBuilder,
        MatomoResponseReader matomoResponseReader,
        CloseableHttpClient httpClient
    ) {
        super(baseUrl, token, matomoRequestBuilder, matomoResponseReader, httpClient);
    }

    @Override
    protected void executeRequest(
        String requestBody, String cookies, BiConsumer<HttpResponse, String> responseConsumer
    ) {
        try (CloseableHttpResponse response = httpClient.execute(createRequest(requestBody, cookies))) {
            responseConsumer.accept(response, requestBody);
        } catch (MatomoClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MatomoClientException("An error occurs sending events to " + baseUrl, ex);
        }
    }

    @Override
    protected HttpPost createRequest(String requestBody, String cookies) {
        HttpPost httpPost = new HttpPost(baseUrl);
        try {
            httpPost.setHeader("Cookie", cookies);
            httpPost.setEntity(new StringEntity(requestBody));
        } catch (UnsupportedEncodingException e) {
            throw new MatomoClientException("Error creating request", e);
        }
        return httpPost;
    }


    @Override
    protected int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

    @Override
    protected String getResponseContent(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            log.error("An error occurs getting the response content", e);
            return "Generic error";
        }
    }
}
