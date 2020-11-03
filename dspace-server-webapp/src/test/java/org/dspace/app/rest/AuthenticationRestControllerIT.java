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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.AuthenticationStatusMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test that covers various authentication scenarios
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
public class AuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};
    public static final String[] SHIB_AND_PASS = {
        "org.dspace.authenticate.ShibAuthentication",
        "org.dspace.authenticate.PasswordAuthentication"
    };
    public static final String[] SHIB_AND_IP = {
        "org.dspace.authenticate.IPAuthentication",
        "org.dspace.authenticate.ShibAuthentication"
    };

    @Before
    public void setup() throws Exception {
        super.setUp();
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups. Anonymous is not considered a direct group.
    public void testStatusAuthenticated() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
                        //We expect the content type to be "application/hal+json;charset=UTF-8"
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")))
        ;

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()))
        ;
    }

    @Test
    public void testStatusNotAuthenticated() throws Exception {

        getClient().perform(get("/api/authn/status"))

                   .andExpect(status().isOk())

                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(false)))
                   .andExpect(jsonPath("$.type", is("status")))
                   .andExpect(header().string("WWW-Authenticate",
                           "password realm=\"DSpace REST API\""));
    }

    @Test
    public void testShibAuthenticatedWithSingleUseCookie() throws Exception {
        //Enable Shibboleth login only
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        String uiURL = configurationService.getProperty("dspace.ui.url");

        // In order to fully simulate a Shibboleth authentication, we'll call
        // /api/authn/shibboleth?redirectUrl=[UI-URL] , with valid Shibboleth request attributes.
        // In this situation, we are mocking how Shibboleth works from our UI (see also ShibbolethRestController):
        // (1) The UI sends the user to Shibboleth to login
        // (2) After a successful login, Shibboleth redirects user to /api/authn/shibboleth?redirectUrl=[url]
        // (3) That triggers generation of the auth token (JWT), and redirects the user to 'redirectUrl', sending along
        //     a single-use cookie containing the auth token.
        // In below call, we're sending a GET request (as that's what a redirect is), without any normal headers
        // (like Origin) to simulate this redirect coming from the Shibboleth server. We are then verifying the user
        // will be redirected to the 'redirectUrl' with a single-use auth cookie
        Cookie authCookie = getClient().perform(get("/api/authn/shibboleth")
                                                    .param("redirectUrl", uiURL)
                                                    .requestAttr("SHIB-MAIL", eperson.getEmail())
                                                    .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
                                       .andExpect(status().is3xxRedirection())
                                       .andExpect(redirectedUrl(uiURL))
                                       .andExpect(cookie().exists(AUTHORIZATION_COOKIE))
                                       .andReturn().getResponse().getCookie(AUTHORIZATION_COOKIE);

        // Verify the single-use cookie now exists & obtain its token for use below
        assertNotNull(authCookie);
        String token = authCookie.getValue();

        // Now, simulate the UI sending the cookie back to the REST API in order to complete the authentication.
        // This is where the single-use cookie will be read, verified & destroyed. After this point, the UI will
        // only use the Authorization header for all future requests.
        // NOTE that this call has an "Origin" matching the UI, to better mock that the request came from there &
        // to verify the single-use auth cookie is valid for the UI's origin.
        getClient().perform(get("/api/authn/status").header("Origin", uiURL)
                                                    .secure(true)
                                                    .cookie(authCookie))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(true)))
                   .andExpect(jsonPath("$.type", is("status")))
                   // Cookie is single use, so its value should now be cleared in response
                   .andExpect(cookie().value(AUTHORIZATION_COOKIE, ""));

        // Now that the single use cookie is cleared, all future requests (from UI)
        // should be made via the Authorization header. So, this tests the token is still valid if sent via headers.
        getClient(token).perform(get("/api/authn/status").header("Origin", uiURL))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(true)))
                   .andExpect(jsonPath("$.type", is("status")));

        // Logout, invalidating the token
        getClient(token).perform(post("/api/authn/logout").header("Origin", uiURL))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testSingleUseCookieCannotBeUsedWithShibDisabled() throws Exception {
        // Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        // Simulate a password authentication
        String token = getAuthToken(eperson.getEmail(), password);

        // Create an authorization Cookie to see if we can use this even though Shibboleth is disabled
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        // Check if cookie can be used to authenticate us via a /status request. This works when Shib is enabled,
        // but this should NOT work (authenticated=false) when Shib is disabled.
        getClient().perform(get("/api/authn/status")
                                .secure(true)
                                .cookie(cookies))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(false)))
                   .andExpect(jsonPath("$.type", is("status")));

        // Test token passed as Header works though (i.e. it's only the cookie that doesn't work)
        // This should return authenticated=true
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testTwoAuthenticationTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        //Sleep so tokens are different
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        assertNotEquals(token1, token2);

        getClient(token1).perform(get("/api/authn/status").param("projection", "full"))

                         .andExpect(status().isOk())

                         //We expect the content type to be "application/hal+json;charset=UTF-8"
                         .andExpect(content().contentType(contentType))
                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(true)))
                         .andExpect(jsonPath("$.type", is("status")))

                         .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                         .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));

        getClient(token2).perform(get("/api/authn/status")
                         .param("projection", "full"))

                         .andExpect(status().isOk())

                         //We expect the content type to be "application/hal+json;charset=UTF-8"
                         .andExpect(content().contentType(contentType))
                         .andExpect(jsonPath("$.okay", is(true)))
                         .andExpect(jsonPath("$.authenticated", is(true)))
                         .andExpect(jsonPath("$.type", is("status")))

                         .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                         .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())));

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

        getClient(token).perform(post("/api/authn/logout"))
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

        getClient(token1).perform(post("/api/authn/logout"));

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
    public void testReuseTokenFromSameOrigin() throws Exception {
        // Simulate a login from a specific, trusted origin
        // NOTE: "https://dspace.org" is added to "rest.cors.allowed-origins" setting for test environment
        String trustedOrigin = "https://dspace.org";
        String token = getAuthTokenWithOrigin(eperson.getEmail(), password, trustedOrigin);

        // Test token works when same "Origin" header is passed
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", trustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token works when same origin is passed in "Referer" header
        getClient(token).perform(get("/api/authn/status")
                                     .header("Referer", trustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));


        // Test token works when same origin is passed in "X-DSpace-UI" custom header
        getClient(token).perform(get("/api/authn/status")
                                     .header("X-DSpace-UI", trustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testReuseTokenFromUntrustedOrigin() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Test token works
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token cannot be used from an *untrusted* Origin
        // (NOTE: this Origin is NOT listed in 'rest.cors.allowed-origins')
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", "https://example.org"))
                        // should result in a 403 error as Spring Security returns that for untrusted origins
                        .andExpect(status().isForbidden());
    }

    @Test
    public void testReuseTokenWithDifferentTrustedOrigin() throws Exception {
        // NOTE: By default we do NOT allow token reuse from different origins (even if origin is trusted)
        // Simulate a login originating from our UI's origin (which is trusted by default)
        String uiUrl = configurationService.getProperty("dspace.ui.url");
        String token = getAuthTokenWithOrigin(eperson.getEmail(), password, uiUrl);
        // NOTE: "https://dspace.org" is added to "rest.cors.allowed-origins" setting for test environment
        String differentTrustedOrigin = "https://dspace.org";

        // Test token works when accessed from same Origin
        getClient(token).perform(get("/api/authn/status")
                                    .header("Origin", uiUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        // Test token does NOT work from a different origin, even though it's trusted
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));


        // Test token does NOT work from different (but trusted) Referer
        getClient(token).perform(get("/api/authn/status")
                                     .header("Referer", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token does NOT work from different (but trusted) UI (X-DSpace-UI sent on all requests from UI)
        getClient(token).perform(get("/api/authn/status")
                                     .header("X-DSpace-UI", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testReuseTokenWithDifferentTrustedOriginWhenAllowed() throws Exception {
        // Disable signing of token with Origin. This allows token to be used cross-origin
        configurationService.setProperty("jwt.login.token.include.origin", "false");

        // Simulate a login originating from our UI's origin (which is trusted by default)
        String uiUrl = configurationService.getProperty("dspace.ui.url");
        String token = getAuthTokenWithOrigin(eperson.getEmail(), password, uiUrl);

        // NOTE: "https://dspace.org" is added to "rest.cors.allowed-origins" setting for test environment
        String differentTrustedOrigin = "https://dspace.org";

        // Test token works from same Origin
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", uiUrl))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token now works from different trusted Origin
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token now works from different trusted Referer
        getClient(token).perform(get("/api/authn/status")
                                     .header("Referer", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token now works from different trusted UI (X-DSpace-UI sent on all requests from UI)
        getClient(token).perform(get("/api/authn/status")
                                     .header("X-DSpace-UI", differentTrustedOrigin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
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
        getClient(token).perform(post("/api/authn/logout"))
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
    public void testShibbolethLoginURLWithDefaultLazyURL() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithServerlURLContainingPort() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("dspace.server.url", "http://localhost:8080/server");
        configurationService.setProperty("authentication-shibboleth.lazysession.secure", false);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://localhost:8080/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%3A8080%2Fserver%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithConfiguredLazyURL() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("authentication-shibboleth.lazysession.loginurl",
                "http://shibboleth.org/Shibboleth.sso/Login");

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://shibboleth.org/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    public void testShibbolethLoginURLWithConfiguredLazyURLWithPort() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        configurationService.setProperty("authentication-shibboleth.lazysession.loginurl",
                "http://shibboleth.org:8080/Shibboleth.sso/Login");

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"http://shibboleth.org:8080/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups
    public void testShibbolethLoginRequestAttribute() throws Exception {
        context.turnOffAuthorisationSystem();
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Create a reviewers group
        Group reviewersGroup = GroupBuilder.createGroup(context)
                .withName("Reviewers")
                .build();

        //Faculty members are assigned to the Reviewers group
        configurationService.setProperty("authentication-shibboleth.role.faculty", "Reviewers");
        context.restoreAuthSystemState();

        getClient().perform(post("/api/authn/login").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened

        String token = getClient().perform(post("/api/authn/login")
                    .requestAttr("SHIB-MAIL", eperson.getEmail())
                    .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Reviewers")));
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups
    public void testShibbolethLoginRequestHeaderWithIpAuthentication() throws Exception {
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_AND_IP);
        configurationService.setProperty("authentication-ip.Administrator", "123.123.123.123");


        getClient().perform(post("/api/authn/login")
                            .header("Referer", "http://my.uni.edu")
                            .with(ip("123.123.123.123")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        "ip realm=\"DSpace REST API\", shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                    .with(ip("123.123.123.123"))
                    .header("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                                    .with(ip("123.123.123.123")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Administrator")));

        //Simulate that a new shibboleth authentication has happened from another IP
        token = getClient().perform(post("/api/authn/login")
                .with(ip("234.234.234.234"))
                .header("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                .with(ip("234.234.234.234")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")));
    }

    @Test
    public void testShibbolethAndPasswordAuthentication() throws Exception {
        //Enable Shibboleth and password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_AND_PASS);

        //Check if WWW-Authenticate header contains shibboleth and password
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
                .andExpect(status().isOk())
                .andExpect(header().string("WWW-Authenticate",
                        "shibboleth realm=\"DSpace REST API\", " +
                                "location=\"https://localhost/Shibboleth.sso/Login?" +
                                "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                                "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\"" +
                                ", password realm=\"DSpace REST API\""));

        //Simulate a password authentication
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Simulate that a shibboleth authentication has happened
        token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace("Bearer ", "");

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out (again)
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

    }

    @Test
    public void testOnlyPasswordAuthenticationWorks() throws Exception {
        //Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        //Check if WWW-Authenticate header contains only
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
            .andExpect(status().isOk())
            .andExpect(header().string("WWW-Authenticate",
                    "password realm=\"DSpace REST API\""));

        //Simulate a password authentication
        String token = getAuthToken(eperson.getEmail(), password);

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testShibbolethAuthenticationDoesNotWorkWithPassOnly() throws Exception {
        //Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        //Check if WWW-Authenticate header contains only password
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
            .andExpect(status().isOk())
            .andExpect(header().string("WWW-Authenticate",
                "password realm=\"DSpace REST API\""));

        //Check if a shibboleth authentication fails
        getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOnlyShibbolethAuthenticationWorks() throws Exception {
        //Enable only Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Check if WWW-Authenticate header contains only shibboleth
        getClient().perform(get("/api/authn/status").header("Referer", "http://my.uni.edu"))
            .andExpect(status().isOk())
            .andExpect(header().string("WWW-Authenticate",
                    "shibboleth realm=\"DSpace REST API\", " +
                            "location=\"https://localhost/Shibboleth.sso/Login?" +
                            "target=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Fshibboleth%3F" +
                            "redirectUrl%3Dhttp%3A%2F%2Fmy.uni.edu\""));

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER);

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testPasswordAuthenticationDoesNotWorkWithShibOnly() throws Exception {
        //Enable only Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        getClient().perform(post("/api/authn/login")
                .param("user", eperson.getEmail())
                .param("password", password))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testShortLivedToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the main session salt doesn't change
        String salt = eperson.getSessionSalt();

        getClient(token).perform(post("/api/authn/shortlivedtokens"))
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.type", is("shortlivedtoken")))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authn/shortlivedtokens")));

        assertEquals(salt, eperson.getSessionSalt());
    }

    @Test
    public void testShortLivedTokenNotAuthenticated() throws Exception {
        getClient().perform(post("/api/authn/shortlivedtokens"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testShortLivedTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isOk());
    }

    @Test
    public void testShortLivedTokenToDownloadBitstreamUnauthorized() throws Exception {
        Bitstream bitstream = createPrivateBitstream();

        context.turnOffAuthorisationSystem();
        EPerson testEPerson = EPersonBuilder.createEPerson(context)
            .withNameInMetadata("John", "Doe")
            .withEmail("UnauthorizedUser@example.com")
            .withPassword(password)
            .build();
        context.restoreAuthSystemState();

        String shortLivedToken = getShortLivedToken(testEPerson);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testLoginTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();

        String loginToken = getAuthToken(eperson.getEmail(), password);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + loginToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testExpiredShortLivedTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");
        String shortLivedToken = getShortLivedToken(eperson);
        Thread.sleep(1);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testShortLivedAndLoginTokenSeparation() throws Exception {
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");

        String token = getAuthToken(eperson.getEmail(), password);
        Thread.sleep(2);
        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", is(true)));
    }

    // TODO: fix the exception. For now we want to verify a short lived token can't be used to login
    @Test(expected = Exception.class)
    public void testLoginWithShortLivedToken() throws Exception {
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(post("/api/authn/login?authentication-token=" + shortLivedToken))
            .andExpect(status().isInternalServerError());
        // TODO: This internal server error needs to be fixed. This should actually produce a forbidden status
        //.andExpect(status().isForbidden());
    }

    @Test
    public void testGenerateShortLivedTokenWithShortLivedToken() throws Exception {
        String shortLivedToken = getShortLivedToken(eperson);

        getClient().perform(post("/api/authn/shortlivedtokens?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());
    }

    private String getShortLivedToken(EPerson requestUser) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        String token = getAuthToken(requestUser.getEmail(), password);
        MvcResult mvcResult = getClient(token).perform(post("/api/authn/shortlivedtokens"))
            .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        Map<String,Object> map = mapper.readValue(content, Map.class);
        return String.valueOf(map.get("token"));
    }

    private Bitstream createPrivateBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. One public items that is readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
            .withTitle("Test")
            .withIssueDate("2010-10-17")
            .withAuthor("Smith, Donald")
            .withSubject("ExtraEntry")
            .build();

        Bundle bundle1 = BundleBuilder.createBundle(context, publicItem1)
            .withName("TEST BUNDLE")
            .build();

        //2. An item restricted to a specific internal group
        Group staffGroup = GroupBuilder.createGroup(context)
            .withName("Staff")
            .addMember(eperson)
            .build();

        String bitstreamContent = "ThisIsSomeDummyText";
        Bitstream bitstream = null;
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstream = BitstreamBuilder.
                createBitstream(context, bundle1, is)
                .withName("Bitstream")
                .withDescription("description")
                .withMimeType("text/plain")
                .withReaderGroup(staffGroup)
                .build();
        }

        context.restoreAuthSystemState();

        return bitstream;
    }
}

