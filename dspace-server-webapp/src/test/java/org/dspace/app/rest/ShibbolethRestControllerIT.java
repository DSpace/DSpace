/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test that cover ShibbolethRestController
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
public class ShibbolethRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void testRedirectToDefaultDspaceUrl() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/shibboleth"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000"));
    }

    @Test
    public void testRedirectToGivenUrl() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/shibboleth")
                .param("redirectUrl", "http://dspace.org"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://dspace.org"));
    }
}
