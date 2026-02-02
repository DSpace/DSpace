/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.matomo.client;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.BiConsumer;

/**
 *
 * {@code MatomoAbstractClient} implementation that handles communication with the Matomo service
 * by using async methods with {@code CompletableFuture}.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class MatomoAsyncClientImpl extends MatomoAbstractClient<HttpClient, HttpRequest, HttpResponse<String>> {

    public MatomoAsyncClientImpl(
        String baseUrl, String token,
        MatomoRequestBuilder matomoRequestBuilder,
        MatomoResponseReader matomoResponseReader
    ) {
        this(
            baseUrl, token, matomoRequestBuilder, matomoResponseReader,
            HttpClient.newBuilder()
                      .version(HttpClient.Version.HTTP_1_1)
                      .connectTimeout(Duration.ofSeconds(5))
                      .proxy(ProxySelector.getDefault())
                      .build()
        );
    }

    public MatomoAsyncClientImpl(
        String baseUrl, String token,
        MatomoRequestBuilder matomoRequestBuilder,
        MatomoResponseReader matomoResponseReader,
        HttpClient httpClient) {
        super(baseUrl, token, matomoRequestBuilder, matomoResponseReader, httpClient);
    }


    @Override
    protected HttpRequest createRequest(String requestBody, String cookies) {
        return HttpRequest.newBuilder(URI.create(baseUrl))
                          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                          .setHeader("Content-Type", "application/json")
                          .setHeader("Cookie", cookies)
                          .build();
    }

    @Override
    protected void executeRequest(
        String requestBody,
        String cookies,
        BiConsumer<HttpResponse<String>, String> responseConsumer
    ) {
        httpClient
            .sendAsync(createRequest(requestBody, cookies), java.net.http.HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> responseConsumer.accept(response, requestBody))
            .exceptionally(this::logError);
    }

    private Void logError(Throwable throwable) {
        log.error("Cannot track this request to Matomo! Check the matomo.tracking.url configured. ", throwable);
        return null;
    }

    protected int getStatusCode(HttpResponse<String> response) {
        return response.statusCode();
    }

    protected String getResponseContent(HttpResponse<String> response) {
        try {
            return response.body();
        } catch (Exception e) {
            log.error("An error occurs getting the response content", e);
            return "Generic error";
        }
    }
}
