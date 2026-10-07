/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import java.net.URISyntaxException;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.identifier.DOI;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DOIResolverClient {

    private static final Logger LOG = LoggerFactory.getLogger(DOIResolverClient.class);

    private final BaseHttpClient client;
    private final String scheme;
    private final String host;
    private final Integer port;

    DOIResolverClient(@Value("${doi.resolver.scheme:https}") String scheme,
                      @Value("${doi.resolver.host:dx.doi.org}") String host,
                      @Value("${doi.resolver.port:#{null}}") Integer port,
                      BaseHttpClient client
    ) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.client = client;
    }

    /**
     * Timeout for API responses. Defaults to 5 seconds as processing can be slow.
     */
    protected int TIMEOUT = 5000;

    /**
     * This method checks if a DOI is already registered.
     *
     * See https://www.crossref.org/documentation/register-maintain-records/verify-your-registration/ for
     * details.
     */
    HttpResponse sendDOIGetRequest(String doi) throws DOIIdentifierException {

        validateDoiFormat(doi);

        var request = buildRequest(doi);

        return doRequest(doi, request);
    }

    private void validateDoiFormat(String doi) {
        if (!doi.startsWith(DOI.SCHEME)) {
            throw new IllegalArgumentException("The DOI " + doi + " does not start with " + DOI.SCHEME);
        }
    }

    private HttpResponse doRequest(String doi, HttpGet httpget) throws DOIIdentifierException {
        return client.sendHttpRequest(httpget, true, buildErrorHandler(doi));
    }

    private HttpGet buildRequest(String doi) {

        var uribuilder = new URIBuilder();

        uribuilder.setScheme(scheme).setHost(host)
                .setPath("/" + doi.substring(DOI.SCHEME.length()));

        if (port != null) {
            uribuilder.setPort(port);
        }

        try {
            // we don't want to follow the redirect, or we don't know if just the handle
            // doesn't resolve or the DOI
            var requestConfig = RequestConfig.custom()
                    .setSocketTimeout(TIMEOUT)
                    .setConnectionRequestTimeout(TIMEOUT)
                    .setRedirectsEnabled(false)
                    .build();

            var httpGet = new HttpGet(uribuilder.build());
            httpGet.setConfig(requestConfig);

            return httpGet;

        } catch (URISyntaxException e) {
            LOG.error("The URL we constructed to check a DOI "
                      + "produced a URISyntaxException. Please check the configuration parameters!");
            LOG.error("The URL was {}.",
                    scheme + "://" + host + (port != null ? (":" + port) : "") + "/"
                    + doi.substring(DOI.SCHEME.length()));
            throw new RuntimeException("The URL we constructed to check a DOI "
                                       + "produced a URISyntaxException. " +
                                       "Please check the configuration parameters!", e);
        }
    }

    private BaseHttpClient.ErrorHandler buildErrorHandler(String doi) {
        return (statusCode, content) -> handleErrorCodes(doi, statusCode, content);
    }

    private void handleErrorCodes(String doi, int statusCode, String content) throws DOIIdentifierException {

        switch (statusCode) {
            case (404): // fallthrough
            case (302): {
                // No error, return (this check is necessary since we include a 'default' case
                return;
            }

            // Catch all other http status code in case we forgot one.
            default: {
                LOG.warn("While checking the DOI {}, we got an unexpected http status code {} and the message \"{}\".",
                        doi, statusCode, content);
                throw new DOIIdentifierException("Unable to parse an answer from Crossref API. " +
                                                 "Please have a look into the DSpace logs.",
                        DOIIdentifierException.BAD_ANSWER);
            }
        }
    }
}
