/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.net.URIBuilder;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@ExtendWith(MockServerExtension.class)
public class HttpConnectionPoolServiceTest
        extends AbstractDSpaceTest {
    private static ConfigurationService configurationService;

    private final MockServerClient mockServerClient;

    public HttpConnectionPoolServiceTest(MockServerClient mockServerClient) {
        this.mockServerClient = mockServerClient;
    }

    @BeforeAll
    public static void initClass() {
        configurationService = DSpaceServicesFactory.getInstance()
                .getConfigurationService();
    }

    /**
     * Test of getClient method, of class HttpConnectionPoolService.
     * @throws java.io.IOException if a connection cannot be closed.
     * @throws java.net.URISyntaxException when an invalid URI is constructed.
     */
    @Test
    public void testGetClient()
            throws IOException, URISyntaxException {
        System.out.println("getClient");

        configurationService.setProperty("solr.client.maxTotalConnections", 2);
        configurationService.setProperty("solr.client.maxPerRoute", 2);
        HttpConnectionPoolService instance = new HttpConnectionPoolService("solr");
        instance.configurationService = configurationService;
        instance.init();

        final String testPath = "/test";
        mockServerClient.when(
                request()
                .withPath(testPath)
        ).respond(
                response()
                .withStatusCode(HttpStatus.OK_200)
        );

        try (CloseableHttpClient httpClient = instance.getClient()) {
            assertNotNull(httpClient, "getClient should always return a client");

            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost("localhost")
                    .setPort(mockServerClient.getPort())
                    .setPath(testPath)
                    .build();
            System.out.println(uri.toString());
            ClassicHttpRequest request = ClassicRequestBuilder.get(uri)
                    .build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(HttpStatus.OK_200, response.getCode(),
                        "Response status should be OK");
            }
        }
    }
}
