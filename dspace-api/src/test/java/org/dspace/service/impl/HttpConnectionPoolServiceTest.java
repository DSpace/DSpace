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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class HttpConnectionPoolServiceTest
        extends AbstractDSpaceTest {
    private static ConfigurationService configurationService;

    private ClientAndServer mockServer;
    private MockServerClient mockServerClient;

    @BeforeAll
    public static void initClass() {
        configurationService = DSpaceServicesFactory.getInstance()
                .getConfigurationService();
    }

    @BeforeEach
    public void setUp() {
        mockServer = ClientAndServer.startClientAndServer();
        mockServerClient = new MockServerClient("localhost", mockServer.getPort());
    }

    @AfterEach
    public void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
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
                    .setPort(mockServer.getPort())
                    .setPath(testPath)
                    .build();
            System.out.println(uri.toString());
            HttpUriRequest request = RequestBuilder.get(uri)
                    .build();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                assertEquals(HttpStatus.OK_200,
                        response.getStatusLine().getStatusCode(),
                        "Response status should be OK");
            }
        }
    }
}
