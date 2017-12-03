/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.Group;
import org.junit.Test;

/**
 * Integration test that covers various authentication scenarios
 *
 * @author Atmire NV (info at atmire dot com)
 */
public class AuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void testStatusAuthenticated() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))

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

        getClient().perform(get("/api/authn/status"))

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

        //Sleep so tokens are different
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        assertNotEquals(token1, token2);

        getClient(token1).perform(get("/api/authn/status"))

                .andExpect(status().isOk())

                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))

                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson.email", is(eperson.getEmail())));

        getClient(token2).perform(get("/api/authn/status"))

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
    public void testTamperingWithToken() throws Exception {
        //Receive a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        //Check it is really valid
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        //The group we try to add to our token
        context.turnOffAuthorisationSystem();
        Group internalGroup = GroupBuilder.createGroup(context)
                .withName("Internal Group")
                .build();
        context.restoreAuthSystemState();

        //Tamper with the token, insert id of group we don't belong to
        String[] jwtSplit = token.split("\\.");

        //We try to inject a special group ID to spoof membership
        String tampered = new String(Base64.getUrlEncoder().encode(
                new String(Base64.getUrlDecoder().decode(
                        token.split("\\.")[1]))
                        .replaceAll("\\[]", "[\"" + internalGroup.getID() + "\"]")
                        .getBytes()));

        String tamperedToken = jwtSplit[0] + "." + tampered + "." + jwtSplit[2];

        //Try to get authenticated with the tampered token
        getClient(tamperedToken).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testLogout() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(get("/api/authn/logout"))
                .andExpect(status().isOk());

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testLogoutInvalidatesAllTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        assertNotEquals(token1, token2);

        getClient(token1).perform(get("/api/authn/logout"));

        getClient(token1).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));


        getClient(token2).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testRefreshToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String newToken = getClient(token).perform(get("/api/authn/login"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Authorization");

        assertNotEquals(token, newToken);

        getClient(newToken).perform(get("/api/authn/status"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testReuseTokenWithDifferentIP() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status")
                .header("X-FORWARDED-FOR", "1.1.1.1"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testFailedLoginResponseCode() throws Exception {
        getClient().perform(get("/api/authn/login")
                .param("user", eperson.getEmail()).param("password", "fakePassword"))

                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginLogoutStatusLink() throws Exception {
        getClient().perform(get("/api/authn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.login.href", endsWith("login")))
                .andExpect(jsonPath("$._links.logout.href", endsWith("logout")))
                .andExpect(jsonPath("$._links.status.href", endsWith("status")));
    }

    /**
     * Check if we can just request a new token after we logged out
     * @throws Exception
     */
    @Test
    public void testLoginAgainAfterLogout() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(get("/api/authn/logout"))
                .andExpect(status().isOk());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));

        //request a new token
        token = getAuthToken(eperson.getEmail(), password);

        //Check if we succesfully authenticated again
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));


    }
}