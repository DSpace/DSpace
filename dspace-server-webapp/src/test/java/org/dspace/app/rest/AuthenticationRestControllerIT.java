/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.lang.Thread.sleep;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_UUID;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        // Default all tests to Password Authentication only
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);
    }

    @Test
    public void testStatusAuthenticatedAsAdmin() throws Exception {
        String token = getAuthToken(admin.getEmail(), password);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(admin.getEmail(), "Administrator")));

        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));

        // Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    @Ignore
    // Ignored until an endpoint is added to return all groups. Anonymous is not considered a direct group.
    public void testStatusAuthenticatedAsNormalUser() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
                        .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")));

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testStatusNotAuthenticated() throws Exception {

        getClient().perform(get("/api/authn/status"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(false)))
                   .andExpect(jsonPath("$.type", is("status")))
                   .andExpect(header().string("WWW-Authenticate",
                           "password realm=\"DSpace REST API\""));
    }

    @Test
    public void testStatusShibAuthenticatedWithCookie() throws Exception {
        //Enable Shibboleth login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        //Simulate that a shibboleth authentication has happened
        String token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace("Bearer ", "");

        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        //Check if we are authenticated with a status request with authorization cookie
        getClient().perform(get("/api/authn/status")
                .secure(true)
                .cookie(cookies))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testStatusPasswordAuthenticatedWithCookie() throws Exception {
        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        // Remove "Bearer " from that token, so that we are left with the token itself
        token = token.replace("Bearer ", "");

        // Save token to an Authorization cookie
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        //Check if we are authenticated with a status request using authorization cookie
        getClient().perform(get("/api/authn/status")
                                .secure(true)
                                .cookie(cookies))
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(true)))
                   .andExpect(jsonPath("$.type", is("status")));
        //Logout
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

        // Verify logout via GET does NOT work (throws a 405)
        getClient(token).perform(get("/api/authn/logout"))
                        .andExpect(status().isMethodNotAllowed())
                        // Verify CSRF token has NOT been changed (as neither the cookie nor header are sent back)
                        .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                        .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        // Verify we are still logged in
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Verify logout via POST works
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent())
                        // New/updated CSRF token should be returned (as both a cookie and header)
                        .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                        .andExpect(header().exists("DSPACE-XSRF-TOKEN"));

        // Verify we are now logged out (authenticated=false)
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
                                          // An auth token refresh should also refresh the CSRF token
                                          // (which should be returned in both a cookie & header).
                                          .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                                          .andExpect(header().exists("DSPACE-XSRF-TOKEN"))
                                          // Whenever our token is changed, we send back 2 cookies
                                          // First cookie will always be empty (removing old value)
                                          // Second cookie has the new token (but unfortunately there's no way to get
                                          // a second cookie of the same name using cookie().value())
                                          // We adopted this behavior from Spring Security's CSRFAuthenticationStrategy
                                          .andExpect(cookie().value("DSPACE-XSRF-COOKIE", ""))
                                          // CSRF Tokens generated by Spring Security are UUIDs
                                          .andExpect(header().string("DSPACE-XSRF-TOKEN", matchesPattern(REGEX_UUID)))
                                          .andReturn().getResponse().getHeader("Authorization");

        assertNotEquals(token, newToken);

        getClient(newToken).perform(get("/api/authn/status"))
                           .andExpect(status().isOk())

                           .andExpect(jsonPath("$.okay", is(true)))
                           .andExpect(jsonPath("$.authenticated", is(true)))
                           .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    // This test is verifying that Spring Security's CSRF protection is working as we expect
    // We must test this using a simple non-GET request, as CSRF Tokens are not validated in a GET request
    public void testRefreshTokenWithInvalidCSRF() throws Exception {
        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        // Remove "Bearer " from that token, so that we are left with the token itself
        token = token.replace("Bearer ", "");

        // Save token to an Authorization cookie
        Cookie[] cookies = new Cookie[1];
        cookies[0] = new Cookie(AUTHORIZATION_COOKIE, token);

        // POSTing to /login should be a valid request...it just refreshes your token (see testRefreshToken())
        // However, in this case, we are POSTing with an *INVALID* CSRF Token in Header.
        getClient().perform(post("/api/authn/login").with(csrf().useInvalidToken().asHeader())
                                                    .secure(true)
                                                    .cookie(cookies))
                   // Should return a 403 Forbidden, for an invalid CSRF token
                   .andExpect(status().isForbidden())
                   // Verify it includes our custom error reason (from DSpaceApiExceptionControllerAdvice)
                   .andExpect(status().reason(containsString("Invalid CSRF token")))
                   // And, a new/updated token should be returned (as both server-side cookie and header)
                   // This is handled by DSpaceAccessDeniedHandler
                   .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                   .andExpect(header().exists("DSPACE-XSRF-TOKEN"));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testLoginChangesCSRFToken() throws Exception {
        // Login via POST, checking the response for a new CSRF Token
        String token = getClient().perform(post("/api/authn/login")
                                          .param("user", eperson.getEmail())
                                          .param("password", password))
                                      // Verify that the CSRF token has been changed
                                      // (as both cookie and header should be sent back)
                                      .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                                      .andExpect(header().exists("DSPACE-XSRF-TOKEN"))
                                      .andReturn().getResponse().getHeader("Authorization");

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    // This test is verifying that Spring Security's CORS settings are working as we expect
    public void testCannotReuseTokenFromUntrustedOrigin() throws Exception {
        // First, get a valid login token
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify token works
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Test token cannot be used from an *untrusted* Origin
        // (NOTE: this Origin is NOT listed in our 'rest.cors.allowed-origins' configuration)
        getClient(token).perform(get("/api/authn/status")
                                     .header("Origin", "https://example.org"))
                        // should result in a 403 error as Spring Security returns that for untrusted origins
                        .andExpect(status().isForbidden());

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testReuseTokenWithDifferentIPWhenIPStored() throws Exception {
        // Enable IP storage in JWT login token
        configurationService.setProperty("jwt.login.token.include.ip", true);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.type", is("status")));

        // Verify a different IP address (behind a proxy, i.e. X-FORWARDED-FOR)
        // is *not* able to authenticate with same token
        getClient(token).perform(get("/api/authn/status")
                                     .header("X-FORWARDED-FOR", "1.1.1.1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Verify a different IP address is *not* able to authenticate with same token
        getClient(token).perform(get("/api/authn/status")
                                    .with(ip("1.1.1.1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.type", is("status")));
    }

    @Test
    public void testReuseTokenWithDifferentIPWhenIPNotStored() throws Exception {
        // Disable IP storage in JWT login token
        configurationService.setProperty("jwt.login.token.include.ip", false);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Verify a different IP address (behind a proxy, i.e. X-FORWARDED-FOR)
        // is able to authenticate with same token
        getClient(token).perform(get("/api/authn/status")
                                     .header("X-FORWARDED-FOR", "1.1.1.1"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Verify a different IP address is able to authenticate with same token
        getClient(token).perform(get("/api/authn/status")
                                     .with(ip("1.1.1.1")))
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
                   .andExpect(status().reason(containsString("Authentication failed")));
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
    public void testShibbolethLoginURLWithServerURLContainingPort() throws Exception {
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
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authn/shortlivedtokens")))
            // Verify generating short-lived token doesn't change our CSRF token
            // (so, neither the CSRF cookie nor header are sent back)
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        assertEquals(salt, eperson.getSessionSalt());
    }

    @Test
    public void testShortLivedTokenWithCSRFSentViaParam() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Same request as prior method, but this time we are sending the CSRF token as a querystring param.
        // NOTE: getClient() method defaults to sending CSRF tokens as Headers, so we are overriding its behavior here
        getClient(token).perform(post("/api/authn/shortlivedtokens").with(csrf()))
            // BECAUSE we sent the CSRF token on querystring, it should be regenerated & a new token
            // is sent back (in cookie and header).
            .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
            .andExpect(header().exists("DSPACE-XSRF-TOKEN"));
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

