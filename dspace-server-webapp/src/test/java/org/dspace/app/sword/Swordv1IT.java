/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.sword;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify that the /sword endpoint is responding as a valid SWORD endpoint.
 * This tests that our dspace-sword module is running at this endpoint.
 * <P>
 * This is a AbstractWebClientIntegrationTest because testing dspace-sword requires
 * running a web server (as dspace-sword makes use of Servlets, not Controllers).
 *
 * @author Tim Donohue
 */
// Ensure the SWORD SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"sword-server.enabled = true"})
public class Swordv1IT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All SWORD paths that we test against
    private final String SERVICE_DOC_PATH = "/sword/servicedocument";
    private final String DEPOSIT_PATH = "/sword/deposit";
    private final String MEDIA_LINK_PATH = "/sword/media-link";

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that SWORDWebConfig is found/available (as this class deploys SWORD)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: SWORDWebConfig is provided by the 'dspace-sword' module
        try {
            Class.forName("org.dspace.app.configuration.SWORDWebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }

        // Ensure SWORD URL configurations are set correctly (based on our integration test server's paths)
        // SWORD validates requests against these configs, and throws a 404 if they don't match the request path
        configurationService.setProperty("sword-server.servicedocument.url", getURL(SERVICE_DOC_PATH));
        configurationService.setProperty("sword-server.deposit.url", getURL(DEPOSIT_PATH));
        configurationService.setProperty("sword-server.media-link.url", getURL(MEDIA_LINK_PATH));
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
        assertThat(response.getHeaders().getContentType().toString(), equalTo("application/atomsvc+xml;charset=UTF-8"));

        // Check for SWORD version in response body
        assertThat(response.getBody(), containsString("<sword:version>1.3</sword:version>"));
    }

    @Test
    public void depositUnauthorizedTest() throws Exception {
        // Attempt to access /deposit endpoint without sending authentication information
        ResponseEntity<String> response = postResponseAsString(DEPOSIT_PATH, null, null, null);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @Ignore
    public void depositTest() throws Exception {
        // TODO: Actually test a full deposit via SWORD.
        // Currently, we are just ensuring the /deposit endpoint exists (see above) and isn't throwing a 404
    }

    @Test
    public void mediaLinkUnauthorizedTest() throws Exception {
        // Attempt to access /media-link endpoint without sending authentication information
        ResponseEntity<String> response = getResponseAsString(MEDIA_LINK_PATH);
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @Ignore
    public void mediaLinkTest() throws Exception {
        // TODO: Actually test a /media-link request.
        // Currently, we are just ensuring the /media-link endpoint exists (see above) and isn't throwing a 404
    }
}

