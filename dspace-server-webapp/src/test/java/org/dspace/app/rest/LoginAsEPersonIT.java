/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

public class LoginAsEPersonIT extends AbstractControllerIntegrationTest {

    @Test
    public void loggedInUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eperson", EPersonMatcher.matchEPersonOnEmail(admin.getEmail())));


    }
    @Test
    public void loggedInAsOtherUserRetrievalTest() throws Exception {

        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                                    .param("projection", "full")
                                    .header("X-On-Behalf-Of", eperson.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._embedded.eperson",
                                            EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));


    }
}
