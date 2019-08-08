/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

public class EmptyRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllTest() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        //Test retrieval of all communities while none exist
        getClient(token).perform(get("/api/core/communities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        //Test retrieval of all collections while none exist
        getClient(token).perform(get("/api/core/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        //Test retrieval of all items while none exist
        getClient(token).perform(get("/api/core/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        //Test retrieval of all bitstreams while none exist
        getClient(token).perform(get("/api/core/bitstreams"))
        .   andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }
}
