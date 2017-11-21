/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test that covers various authentication scenarios
 */
public class AuthenticationRestControllerTest extends AbstractControllerIntegrationTest {

    @Test
    public void testStatusAuthenticated() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/status"))

                .andExpect(status().isOk())

                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))

                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson.email", is(eperson.getEmail())));
    }

    @Test
    public void testStatusNotAuthenticated() throws Exception {

        getClient().perform(get("/api/status"))

                .andExpect(status().isOk())

                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testTwoAuthenticationTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);
        String token2 = getAuthToken(eperson.getEmail(), password);

        getClient(token1).perform(get("/api/status"))

                .andExpect(status().isOk())

                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))

                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson.email", is(eperson.getEmail())));

        getClient(token2).perform(get("/api/status"))

                .andExpect(status().isOk())

                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))

                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson.email", is(eperson.getEmail())));

    }

    @Test
    public void testLogout() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/logout"));

        getClient(token).perform(get("/api/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testLogoutInvalidatesAllTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);
        String token2 = getAuthToken(eperson.getEmail(), password);

        getClient(token1).perform(get("/api/logout"));

        getClient(token2).perform(get("/api/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));


        getClient(token2).perform(get("/api/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testRefreshToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/login"))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", "Bearer .*"));

    }

    @Test
    public void testReuseTokenWithDifferentIP() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/status").header("X-FORWARDED-FOR", "1.1.1.1"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testFailedLoginResponseCode() throws Exception {
        getClient().perform(get("/api/login").param("user", eperson.getEmail()).param("password", "fakePassword"))
                .andExpect(status().is(401));
    }
}