/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.sword;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.dspace.app.rest.test.AbstractWebClientIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to test the /sword endpoint which loads/embeds the SWORD webapp into the REST API.
 * This is a AbstractWebClientIntegrationTest because testing the SWORD webapp requires
 * running a web server (as the SWORD webapp makes use of Servlets, not Controllers).
 *
 * @author Tim Donohue
 */
// Ensure the SWORD SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"sword-server.enabled = true"})
public class Swordv1IT extends AbstractWebClientIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void serviceDocumentUnauthorizedTest() throws Exception {
        // Attempt to load the ServiceDocument without first authenticating
        ResponseEntity<String> response = getResponseAsString("/sword/servicedocument");
        // Expect a 401 response code
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        // Before we can test the ServiceDocument, we must be sure the URL is configured properly
        // If the configured URL doesn't match exactly, SWORD will respond with a 404, even if authentication succeeds
        configurationService.setProperty("sword-server.servicedocument.url", getURL("/sword/servicedocument"));

        // Attempt to load the ServiceDocument as an Admin user.
        ResponseEntity<String> response = getResponseAsString("/sword/servicedocument",
                                                              admin.getEmail(), password);
        // Expect a 200 response code, and an ATOM UTF-8 document
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType().toString(), equalTo("application/atomsvc+xml;charset=UTF-8"));
    }

}

