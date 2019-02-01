/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.sword2;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to test the /swordv2 endpoint which loads/embeds the SWORDv2 webapp into the REST API.
 * This is a AbstractWebClientIntegrationTest because testing the SWORDv2 webapp requires
 * running a web server (as the SWORDv2 webapp makes use of Servlets, not Controllers).
 *
 * @author Tim Donohue
 */
// Ensure the SWORDv2 SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"swordv2-server.enabled = true"})
public class Swordv2IT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All SWORD v2 paths that we test against
    private final String SERVICE_DOC_PATH = "/swordv2/servicedocument";

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that SWORDv2WebConfig is found/available (as this class deploys SWORDv2)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: SWORDv2WebConfig is provided by the 'dspace-swordv2' module
        try {
            Class.forName("org.dspace.app.configuration.SWORDv2WebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Ensure SWORDv2 URL configurations are set correctly (based on our integration test server's paths)
        // SWORDv2 validates requests against these configs, and throws a 404 if they don't match the request path
        configurationService.setProperty("swordv2-server.servicedocument.url", getURL(SERVICE_DOC_PATH));
    }

    @Test
    public void serviceDocumentUnauthorizedTest() throws Exception {
        // Attempt to load the ServiceDocument without first authenticating
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        // Attempt to load the ServiceDocument as an Admin user.
        ResponseEntity<String> response = getResponseAsString(SERVICE_DOC_PATH,
                                                              admin.getEmail(), password);
        // Expect a 200 response code, and an ATOM UTF-8 document
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType().toString(),
                   equalTo("application/atomserv+xml;charset=UTF-8"));

        // Check for SWORD version in response body
        assertThat(response.getBody(),
                   containsString("<version xmlns=\"http://purl.org/net/sword/terms/\">2.0</version>"));
    }
}

