/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.opensearch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test to test the /opensearch endpoint
 *
 * @author Oliver Goldschmidt (o dot goldschmidt at tuhh dot de)
 */
public class OpenSearchControllerDisabledIT extends AbstractControllerIntegrationTest {

    private ConfigurationService configurationService;

    @Before
    public void init() throws Exception {
        // disable OpenSearch by configuration to test the disabled behaviour although its active in test config
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        configurationService.setProperty("websvc.opensearch.enable", false);
    }

    @Test
    public void searchTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/search")
                                .param("query", "dog"))
                   //The status has to be 404 Not Found
                   .andExpect(status().isNotFound())
                   //We expect the content type to be "text/html"
                   .andExpect(content().contentType("text/html;charset=UTF-8"))
                   .andExpect(content().string("OpenSearch Service is disabled"))
        ;
    }

    @Test
    public void serviceDocumentTest() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/opensearch/service"))
                   //The status has to be 404 Not Found
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType("text/html;charset=UTF-8"))
                   .andExpect(content().string("OpenSearch Service is disabled"))
        ;
    }
}
