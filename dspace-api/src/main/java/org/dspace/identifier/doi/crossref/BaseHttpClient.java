/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import java.io.IOException;
import javax.annotation.Nullable;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Low-level HTTP operations for CrossRef/DOI registries.
 */
@Component
class BaseHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(BaseHttpClient.class);

    private final DSpaceHttpClientFactory httpClientFactory;

    BaseHttpClient(DSpaceHttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    interface ErrorHandler {
        void handleError(int statusCode, String content) throws DOIIdentifierException;
    }

    /**
     * Sends a request to a DOI registry.
     *
     * @param req The request to be sent.
     * @return A {@link HttpResponse} object containing information about the registry response.
     * @throws DOIIdentifierException if an error occurs during request submission.
     */
    HttpResponse sendHttpRequest(HttpUriRequest req,
                                 boolean disableRedirects,
                                 ErrorHandler handleError) throws DOIIdentifierException {

        /*
            for request logging try increasing loglevels:

            org.apache.http=DEBUG
            org.apache.http.wire=DEBUG
         */

        try (var client = newClient(disableRedirects)) {
            try (var response = client.execute(req)) {
                var status = response.getStatusLine();
                var statusCode = status.getStatusCode();
                var content = extractContent(response);

                handleError.handleError(statusCode, content);

                var url = extractRedirectUrlFromResponse(response);

                return new HttpResponse(statusCode, content, url);
            }
        } catch (IOException e) {
            LOG.warn("Caught an IOException: ", e);
            throw new RuntimeException(e);
        }
    }

    private CloseableHttpClient newClient(boolean disableRedirects) {
        if (disableRedirects) {
            return httpClientFactory.build(HttpClientBuilder::disableRedirectHandling);
        } else {
            return httpClientFactory.build();
        }
    }

    @Nullable
    private String extractContent(CloseableHttpResponse response) throws IOException {
        var entity = response.getEntity();
        if (entity != null) {
            return EntityUtils.toString(entity, "UTF-8");
        }
        return null;
    }

    @Nullable
    private String extractRedirectUrlFromResponse(org.apache.http.HttpResponse response) {
        if (response.containsHeader(HttpHeaders.LOCATION)) {
            return response.getFirstHeader(HttpHeaders.LOCATION).getValue();
        }

        return null;
    }
}
