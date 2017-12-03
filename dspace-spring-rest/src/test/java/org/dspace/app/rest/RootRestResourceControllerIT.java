/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test for the {@link RootRestResourceController}
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class RootRestResourceControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void listDefinedEndpoint() throws Exception {

        //When we call the root endpoint
        getClient().perform(get("/api"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //Check that all required root links are present and that they are absolute
                .andExpect(jsonPath("$._links.bitstreamformats.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.bitstreams.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.browses.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.collections.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.communities.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.epersons.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.groups.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.items.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.metadatafields.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.metadataschemas.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.sites.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.authn.href", startsWith(REST_SERVER_URL)))
                ;
    }

}