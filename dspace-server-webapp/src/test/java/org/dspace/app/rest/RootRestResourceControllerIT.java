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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Integration test for the {@link RootRestResourceController}
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class RootRestResourceControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void serverPropertiesTest() throws Exception {
      //When we call the root endpoint
        getClient().perform(get("/api"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.dspaceURL", Matchers.is("http://localhost:3000")))
                   .andExpect(jsonPath("$.dspaceName", Matchers.is("DSpace at My University")))
                   .andExpect(jsonPath("$.dspaceRest", Matchers.is(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$.type", Matchers.is("root")));
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
                   .andExpect(jsonPath("$._links.authorities.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.bitstreamformats.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.bitstreams.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.browses.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.collections.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.communities.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.epersons.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.groups.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.items.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.metadatafields.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.metadataschemas.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.resourcepolicies.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.sites.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.submissiondefinitions.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.submissionforms.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.submissionsections.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.submissionuploads.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.workspaceitems.href", startsWith(BASE_REST_SERVER_URL)))
                   .andExpect(jsonPath("$._links.authn.href", startsWith(BASE_REST_SERVER_URL)))
        ;
    }

}