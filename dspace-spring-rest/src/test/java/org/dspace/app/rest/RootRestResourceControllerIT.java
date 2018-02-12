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

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test for the {@link RootRestResourceController}
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class RootRestResourceControllerIT extends AbstractControllerIntegrationTest {

    protected String getRestCategory() {
        return RestModel.CORE;
    }

    @Test
    public void listDefinedEndpoint() throws Exception {

        //When we call the root endpoint
        getClient().perform(get("/api"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //Check that all required root links are present and that they are absolute
                .andExpect(jsonPath("$._links.i:authorities.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:bitstreamformats.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:bitstreams.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.d:browses.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:collections.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:communities.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.p:epersons.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.p:groups.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:items.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:metadatafields.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:metadataschemas.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.az:resourcePolicies.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:sites.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.cf:submissiondefinitions.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.cf:submissionforms.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.cf:submissionsections.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.cf:submissionuploads.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.s:workspaceitems.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._links.c:authn.href", startsWith(REST_SERVER_URL)))
                ;
    }

}