/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration tests for the common "page" and "size" request parameters described in the REST contract.
 */
public class PaginationParametersIT extends AbstractControllerIntegrationTest {

    @Test
    public void negativePageIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities").param("page", "-1"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void zeroSizeIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities").param("size", "0"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void negativeSizeIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities").param("size", "-5"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void nonNumericPageIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities").param("page", "abc"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void nonNumericSizeIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities").param("size", "def"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void validPageAndSizeAreAccepted() throws Exception {
        getClient().perform(get("/api/core/communities").param("page", "0").param("size", "1"))
                   .andExpect(status().isOk());
    }

    @Test
    public void invalidSizeOnSearchEndpointIsBadRequest() throws Exception {
        getClient().perform(get("/api/core/communities/search/top").param("size", "0"))
                   .andExpect(status().isBadRequest());
    }

    @Test
    public void invalidPageOnDiscoverEndpointIsBadRequest() throws Exception {
        getClient().perform(get("/api/discover/search/objects").param("page", "-1"))
                   .andExpect(status().isBadRequest());
    }

}
