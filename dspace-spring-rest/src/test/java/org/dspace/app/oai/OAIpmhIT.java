/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.oai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.Assume;
import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test to verify the /oai endpoint is responding as a valid OAI-PMH endpoint.
 * This tests that our dspace-oai module is running at this endpoint.
 * <P>
 * This is an AbstractControllerIntegrationTest because dspace-oai makes use of Controllers.
 *
 * @author Tim Donohue
 */
// Ensure the OAI SERVER IS ENABLED before any tests run.
// This annotation overrides default DSpace config settings loaded into Spring Context
@TestPropertySource(properties = {"oai.enabled = true"})
public class OAIpmhIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    // All OAI-PMH paths that we test against
    private final String ROOT_PATH = "/oai";
    private final String ROOT_REQUEST = "/oai/request";

    @Before
    public void onlyRunIfConfigExists() {
        // These integration tests REQUIRE that OAIWebConfig is found/available (as this class deploys OAI)
        // If this class is not available, the below "Assume" will cause all tests to be SKIPPED
        // NOTE: OAIWebConfig is provided by the 'dspace-oai' module
        try {
            Class.forName("org.dspace.app.configuration.OAIWebConfig");
        } catch (ClassNotFoundException ce) {
            Assume.assumeNoException(ce);
        }
    }

    @Test
    public void oaiRootTest() throws Exception {
        // Attempt to call the root endpoint
        getClient().perform(get(ROOT_PATH))
                    // Expect a 400 response code (OAI requires a context)
                   .andExpect(status().isBadRequest())
        ;
    }


    @Test
    public void oaiRootIdentifyTest() throws Exception {
        // Attempt to make an Identify request to root context
        getClient().perform(get(ROOT_REQUEST).param("verb", "Identify"))
                   // Expect a 200 response code
                   .andExpect(status().isOk())
                   // Expect the content type to be "text/xml"
                   .andExpect(content().contentType("text/xml"))
                   // Expect <scheme>oai</scheme>
                   .andExpect(xpath("OAI-PMH/Identify/description/oai-identifier/scheme").string("oai"))
                   // Expect protocol version 2.0
                   .andExpect(xpath("OAI-PMH/Identify/protocolVersion").string("2.0"))
                   // Expect repositoryName to be the same as "dspace.name" config
                   .andExpect(xpath("OAI-PMH/Identify/repositoryName")
                                  .string(configurationService.getProperty("dspace.name")))
        ;
    }

}

