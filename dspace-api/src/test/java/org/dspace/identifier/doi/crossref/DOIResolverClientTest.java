/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import static org.assertj.core.api.Assertions.assertThat;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DOIResolverClientTest {

    private MockWebServer server;
    private DOIResolverClient client;

    @BeforeEach
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        var baseClient = TestClient.baseHttpClientForTest();

        client = new DOIResolverClient("http", server.getHostName(), server.getPort(), baseClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
    }

    @Test
    public void sendDOIGetRequest_followsConfigAndReturnsRedirectInfo() throws Exception {
        // Arrange: enqueue a 302 with a Location header as dx.doi would do for a registered DOI
        var redirectUrl = "https://example.org/handle/123456789";
        server.enqueue(new MockResponse.Builder()
                .code(302)
                .setHeader("Location", redirectUrl)
                .body("Found").build());

        var doi = "doi:10.1234/abcd";

        var response = client.sendDOIGetRequest(doi);

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.url()).isEqualTo(redirectUrl);

        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getTarget()).isEqualTo("/10.1234/abcd");
    }

    @Test
    public void sendDOIGetRequest_handles404WithoutException() throws Exception {
        // Arrange: enqueue a 404 Not Found to simulate unknown DOI
        server.enqueue(new MockResponse.Builder()
                .code(404)
                .body("Not Found").build());

        var doi = "doi:10.9999/missing";

        var response = client.sendDOIGetRequest(doi);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.url()).isNull();

        var recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getTarget()).isEqualTo("/10.9999/missing");
    }
}