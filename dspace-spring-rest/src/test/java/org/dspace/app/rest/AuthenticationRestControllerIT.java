/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.lang.Thread.sleep;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test that covers various authentication scenarios
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class AuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};

    @Before
    public void setup() throws Exception {
        super.setUp();
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);
   }

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
                        .andExpect(status().isNoContent());

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

        String newToken = getClient(token).perform(post("/api/authn/login"))
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
        getClient().perform(post("/api/authn/login")
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
     *
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
                        .andExpect(status().isNoContent());

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

    @Test
    public void testLoginEmptyRequest() throws Exception {
        getClient().perform(post("/api/authn/login"))
                   .andExpect(status().isUnauthorized())
                   .andExpect(status().reason(containsString("Login failed")));
    }

    @Test
    public void testLoginGetRequest() throws Exception {
       getClient().perform(get("/api/authn/login")
            .param("user", eperson.getEmail())
            .param("password", password))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void testShibbolethLoginRequest() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Location", "/Shibboleth.sso/Login?target=http%3A%2F%2Fmy.uni.edu"))
                .andReturn().getResponse().getHeader("Location");

        //Simulate that a shibboleth authentication has happened

        String token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

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

}
