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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Base64;
import java.util.Map;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.impl.CanChangePasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.matcher.AuthenticationStatusMatcher;
import org.dspace.app.rest.matcher.AuthorizationMatcher;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.GroupMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.model.AuthnRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authenticate.OrcidAuthenticationBean;
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
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
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

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Autowired
    private OrcidAuthenticationBean orcidAuthentication;

    @Autowired
    private Utils utils;

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};
    public static final String[] ORCID_ONLY = { "org.dspace.authenticate.OrcidAuthentication" };
    public static final String[] SHIB_AND_PASS = {
        "org.dspace.authenticate.ShibAuthentication",
        "org.dspace.authenticate.PasswordAuthentication"
    };
    public static final String[] SHIB_AND_IP = {
        "org.dspace.authenticate.IPAuthentication",
        "org.dspace.authenticate.ShibAuthentication"
    };
    public static final String[] PASS_AND_IP = {
            "org.dspace.authenticate.PasswordAuthentication",
            "org.dspace.authenticate.IPAuthentication"
        };

    // see proxies.trusted.ipranges in local.cfg
    public static final String TRUSTED_IP = "7.7.7.7";
    public static final String UNTRUSTED_IP = "8.8.8.8";

    private Authorization authorization;
    private EPersonRest ePersonRest;
    private final String feature = CanChangePasswordFeature.NAME;


    @Before
    public void setup() throws Exception {
        super.setUp();

        AuthorizationFeature canChangePasswordFeature = authorizationFeatureService.find(CanChangePasswordFeature.NAME);
        ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);
        authorization = new Authorization(eperson, canChangePasswordFeature, ePersonRest);

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
                        .andExpect(jsonPath("$.authenticationMethod", is("password")))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(admin.getEmail(), "Administrator")));

        getClient(token).perform(get("/api/authz/authorizations/" + authorization.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                                AuthorizationMatcher.matchAuthorization(authorization))));

        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));

        // Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    /**
     * This test verifies:
     * - that a logged in via password user finds the expected specialGroupPwd in _embedded.specialGroups;
     * - that a logged in via password and specific IP user finds the expected specialGroupPwd and specialGroupIP
     *   in _embedded.specialGroups;
     * - that a not logged in user with a specific IP finds the expected specialGroupIP in _embedded.specialGroups;
     * @throws Exception
     */
    @Test
    public void testStatusGetSpecialGroups() throws Exception {
        context.turnOffAuthorisationSystem();

        Group specialGroupPwd = GroupBuilder.createGroup(context)
                .withName("specialGroupPwd")
                .build();
        Group specialGroupIP = GroupBuilder.createGroup(context)
               .withName("specialGroupIP")
               .build();

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_AND_IP);
        configurationService.setProperty("authentication-password.login.specialgroup","specialGroupPwd");
        configurationService.setProperty("authentication-ip.specialGroupIP", "123.123.123.123");
        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
            .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.authenticationMethod", is("password")))
            .andExpect(jsonPath("$.type", is("status")))
            .andExpect(jsonPath("$._links.specialGroups.href", startsWith(REST_SERVER_URL)))
            .andExpect(jsonPath("$._embedded.specialGroups._embedded.specialGroups",
                Matchers.containsInAnyOrder(
                        GroupMatcher.matchGroupWithName("specialGroupPwd"))));

        // try the special groups link endpoint in the same scenario than above
        getClient(token).perform(get("/api/authn/status/specialGroups").param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.specialGroups",
                Matchers.containsInAnyOrder(
                        GroupMatcher.matchGroupWithName("specialGroupPwd"))));

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                .with(ip("123.123.123.123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
            .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.authenticationMethod", is("password")))
            .andExpect(jsonPath("$.type", is("status")))
            .andExpect(jsonPath("$._links.specialGroups.href", startsWith(REST_SERVER_URL)))
            .andExpect(jsonPath("$._embedded.specialGroups._embedded.specialGroups",
                    Matchers.containsInAnyOrder(
                            GroupMatcher.matchGroupWithName("specialGroupPwd"),
                            GroupMatcher.matchGroupWithName("specialGroupIP"))));

        // try the special groups link endpoint in the same scenario than above
        getClient(token).perform(get("/api/authn/status/specialGroups").param("projection", "full")
                .with(ip("123.123.123.123")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.specialGroups",
                Matchers.containsInAnyOrder(
                        GroupMatcher.matchGroupWithName("specialGroupPwd"),
                        GroupMatcher.matchGroupWithName("specialGroupIP"))));

        getClient().perform(get("/api/authn/status").param("projection", "full").with(ip("123.123.123.123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", AuthenticationStatusMatcher.matchFullEmbeds()))
            // fails due to bug https://github.com/DSpace/DSpace/issues/8274
            //.andExpect(jsonPath("$", AuthenticationStatusMatcher.matchLinks()))
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(false)))
            .andExpect(jsonPath("$._embedded.specialGroups._embedded.specialGroups",
                    Matchers.containsInAnyOrder(GroupMatcher.matchGroupWithName("specialGroupIP"))));

        // try the special groups link endpoint in the same scenario than above
        getClient().perform(get("/api/authn/status/specialGroups").param("projection", "full")
                .with(ip("123.123.123.123")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.specialGroups",
                Matchers.containsInAnyOrder(
                        GroupMatcher.matchGroupWithName("specialGroupIP"))));
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
                        .andExpect(jsonPath("$.authenticationMethod", is("password")))
                        .andExpect(jsonPath("$.type", is("status")))

                        .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                        .andExpect(jsonPath("$._embedded.eperson",
                                EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")));

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));

        getClient(token).perform(get("/api/authz/authorizations/" + authorization.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                                AuthorizationMatcher.matchAuthorization(authorization))));

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
                   .andExpect(jsonPath("$.authenticationMethod").doesNotExist())
                   .andExpect(jsonPath("$.type", is("status")))
                   .andExpect(header().string("WWW-Authenticate",
                           "password realm=\"DSpace REST API\""));

        getClient().perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testStatusShibAuthenticatedWithCookie() throws Exception {
        //Enable Shibboleth login only
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        String uiURL = configurationService.getProperty("dspace.ui.url");

        // In order to fully simulate a Shibboleth authentication, we'll call
        // /api/authn/shibboleth?redirectUrl=[UI-URL] , with valid Shibboleth request attributes.
        // In this situation, we are mocking how Shibboleth works from our UI (see also ShibbolethLoginFilter):
        // (1) The UI sends the user to Shibboleth to login
        // (2) After a successful login, Shibboleth redirects user to /api/authn/shibboleth?redirectUrl=[url]
        // (3) That triggers generation of the auth token (JWT), and redirects the user to 'redirectUrl', sending along
        //     a temporary cookie containing the auth token.
        // In below call, we're sending a GET request (as that's what a redirect is), with a Referer of a "fake"
        // Shibboleth server to simulate this request coming back from Shibboleth (after a successful login).
        // We are then verifying the user will be redirected to the 'redirectUrl' with a single-use auth cookie
        // (NOTE: Additional tests of this /api/authn/shibboleth endpoint can be found in ShibbolethLoginFilterIT)
        Cookie authCookie = getClient().perform(get("/api/authn/shibboleth")
                .header("Referer", "https://myshib.example.com")
                .param("redirectUrl", uiURL)
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(uiURL))
                // Verify that the CSRF token has NOT been changed. Creating the auth cookie should NOT change our CSRF
                // token. The CSRF token should only change when we call /login with the cookie (see later in this test)
                .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"))
                .andExpect(cookie().exists(AUTHORIZATION_COOKIE))
                .andReturn().getResponse().getCookie(AUTHORIZATION_COOKIE);

        // Verify the temporary cookie now exists & obtain its token for use below
        assertNotNull(authCookie);
        String token = authCookie.getValue();

        // This step is _not required_ to successfully authenticate, but it mocks the behavior of our UI & HAL Browser.
        // We'll send a "/status" request to the REST API with our auth cookie. This should return that we have a
        // *valid* authentication (as auth cookie is valid), however the cookie will remain. To complete the login
        // process we MUST call the "/login" endpoint (see the next step in this test).
        // (NOTE that this call has an "Origin" matching the UI, to better mock that the request came from there &
        // to verify the temporary auth cookie is valid for the UI's origin.)
        getClient().perform(get("/api/authn/status").header("Origin", uiURL)
                                                              .secure(true)
                                                              .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                .andExpect(jsonPath("$.type", is("status")))
                // Verify that the CSRF token has NOT been changed... status checks won't change the token
                // (only login/logout will)
                .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

        // To complete the authentication process, we pass our auth cookie to the "/login" endpoint.
        // This is where the temporary cookie will be read, verified & destroyed. After this point, the UI will
        // only use the 'Authorization' header for all future requests.
        // (NOTE that this call has an "Origin" matching the UI, to better mock that the request came from there &
        // to verify the temporary auth cookie is valid for the UI's origin.)
        String headerToken = getClient().perform(post("/api/authn/login").header("Origin", uiURL)
                                                                                   .secure(true)
                                                                                   .cookie(authCookie))
                .andExpect(status().isOk())
                // Verify the Auth cookie has been destroyed
                .andExpect(cookie().value(AUTHORIZATION_COOKIE, ""))
                // Verify Authorization header is returned
                .andExpect(header().exists(AUTHORIZATION_HEADER))
                // Verify that the CSRF token has been changed
                // (as both cookie and header should be sent back)
                .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                .andExpect(header().exists("DSPACE-XSRF-TOKEN"))
                .andReturn().getResponse()
                .getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        // Verify that the token in the returned header has the *same claims* as the auth Cookie token
        // NOTE: We test claim equality because the token's expiration date may change during this request. If it does
        // change, then the tokens will look different even though the claims are the same.
        assertTrue("Check tokens " + token + " and " + headerToken + " have same claims",
                   tokenClaimsEqual(token, headerToken));

        // Now that the auth cookie is cleared, all future requests (from UI)
        // should be made via the Authorization header. So, this tests the token is still valid if sent via header.
        getClient(headerToken).perform(get("/api/authn/status").header("Origin", uiURL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

        //Logout, invalidating the token
        getClient(headerToken).perform(post("/api/authn/logout").header("Origin", uiURL))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testShibbolethEndpointCannotBeUsedWithShibDisabled() throws Exception {
        // Enable only password login
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        String uiURL = configurationService.getProperty("dspace.ui.url");

        // Verify /api/authn/shibboleth endpoint does not work
        // NOTE: this is the same call as in testStatusShibAuthenticatedWithCookie())
        String token = getClient().perform(get("/api/authn/shibboleth")
                .header("Referer", "https://myshib.example.com")
                .param("redirectUrl", uiURL)
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getHeader("Authorization");

        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.authenticationMethod").doesNotExist());

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.page.totalElements", is(0)))
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    // NOTE: This test is similar to testStatusShibAuthenticatedWithCookie(), but proves the same process works
    // for Password Authentication in theory (NOTE: at this time, there's no way to create an auth cookie via the
    // Password Authentication process).
    @Test
    public void testStatusPasswordAuthenticatedWithCookie() throws Exception {
        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

        // Fake the creation of an auth cookie, just for testing. (Currently, it's not possible to create an auth cookie
        // via Password auth, but this test proves it would work if enabled)
        Cookie authCookie = new Cookie(AUTHORIZATION_COOKIE, token);

        // Now, similar to how both the UI & Hal Browser authentication works, send a "/status" request to the REST API
        // with our auth cookie. This should return that we *have a valid* authentication (in the auth cookie).
        // However, this is just a validation check, so this auth cookie will remain. To complete the login process
        // we'll need to call the "/login" endpoint (see the next step in this test).
        getClient().perform(get("/api/authn/status").secure(true).cookie(authCookie))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.okay", is(true)))
                   .andExpect(jsonPath("$.authenticated", is(true)))
                   .andExpect(jsonPath("$.type", is("status")))
                   // Verify that the CSRF token has NOT been changed... status checks won't change the token
                   // (only login/logout will)
                   .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                   .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        // To complete the authentication process, we pass our auth cookie to the "/login" endpoint.
        // This is where the temporary cookie will be read, verified & destroyed. After this point, the UI will
        // only use the Authorization header for all future requests.
        String headerToken = getClient().perform(post("/api/authn/login").secure(true).cookie(authCookie))
                    .andExpect(status().isOk())
                    // Verify the Auth cookie has been destroyed
                    .andExpect(cookie().value(AUTHORIZATION_COOKIE, ""))
                    // Verify Authorization header is returned
                    .andExpect(header().exists(AUTHORIZATION_HEADER))
                    // Verify that the CSRF token has been changed
                    // (as both cookie and header should be sent back)
                    .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
                    .andExpect(header().exists("DSPACE-XSRF-TOKEN"))
                    .andReturn().getResponse()
                    .getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        // Verify that the token in the returned header has the *same claims* as the auth Cookie token
        // NOTE: We test claim equality because the token's expiration date may change during this request. If it does
        // change, then the tokens will look different even though the claims are the same.
        assertTrue("Check tokens " + token + " and " + headerToken + " have same claims",
                   tokenClaimsEqual(token, headerToken));

        // Now that the auth cookie is cleared, all future requests (from UI)
        // should be made via the Authorization header. So, this tests the token is still valid if sent via header.
        getClient(headerToken).perform(get("/api/authn/status"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(contentType))
                    .andExpect(jsonPath("$.okay", is(true)))
                    .andExpect(jsonPath("$.authenticated", is(true)))
                    .andExpect(jsonPath("$.type", is("status")));

        // Logout, invalidating the token
        getClient(headerToken).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    public void testTwoAuthenticationTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        // Sleep for >1sec ensures the tokens are different. Because expiration date in the token includes a timestamp,
        // waiting for >1sec will result in a different expiration, and therefore a slightly different token.
        sleep(1200);

        String token2 = getAuthToken(eperson.getEmail(), password);

        // Tokens should be different
        assertNotEquals(token1, token2);

        // However, tokens should contain the same claims
        assertTrue("Check tokens " + token1 + " and " + token2 + " have same claims",
                   tokenClaimsEqual(token1, token2));

        // BOTH tokens should be valid, as they represent the same authenticated user's "session".
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

        // Logout, this will invalidate both tokens
        // NOTE: invalidation of both tokens is tested in testLogoutInvalidatesAllTokens()
        getClient(token1).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());

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

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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
                        .andExpect(jsonPath("$.authenticationMethod").doesNotExist())
                        .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void testLogoutInvalidatesAllTokens() throws Exception {
        String token1 = getAuthToken(eperson.getEmail(), password);

        // Sleep for >1sec ensures the tokens are different. Because expiration date in the token includes a timestamp,
        // waiting for >1sec will result in a different expiration, and therefore a slightly different token.
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

        // Sleep for >1sec ensures the tokens are different. Because expiration date in the token includes a timestamp,
        // waiting for >1sec will result in a different expiration, and therefore a slightly different token.
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
                                          .andReturn().getResponse()
                                          .getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        // Tokens should be different
        assertNotEquals(token, newToken);

        // However, tokens should contain the same claims
        assertTrue("Check tokens " + token + " and " + newToken + " have same claims",
                   tokenClaimsEqual(token, newToken));

        // Verify new token is valid
        getClient(newToken).perform(get("/api/authn/status"))
                           .andExpect(status().isOk())

                           .andExpect(jsonPath("$.okay", is(true)))
                           .andExpect(jsonPath("$.authenticated", is(true)))
                           .andExpect(jsonPath("$.authenticationMethod", is("password")))
                           .andExpect(jsonPath("$.type", is("status")));

        getClient(newToken).perform(get("/api/authz/authorizations/" + authorization.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                                AuthorizationMatcher.matchAuthorization(authorization))));

        // Logout, this will invalidate both tokens
        // NOTE: invalidation of both tokens is tested in testLogoutInvalidatesAllTokens()
        getClient(newToken).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    // This test is verifying that Spring Security's CSRF protection is working as we expect
    // We must test this using a simple non-GET request, as CSRF Tokens are not validated in a GET request
    public void testRefreshTokenWithInvalidCSRF() throws Exception {
        // Login via password to retrieve a valid token
        String token = getAuthToken(eperson.getEmail(), password);

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
                                      .andReturn().getResponse()
                                      .getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());
    }

    @Test
    // This test (and next) is verifying that Spring Security's CORS settings are working as we expect
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
    // This test (and previous) is verifying that Spring Security's CORS settings are working as we expect
    public void testCannotAuthenticateFromUntrustedOrigin() throws Exception {
        // Post a valid username & password from an *untrusted* Origin
        getClient().perform(post("/api/authn/login").header("Origin", "https://example.org")
                                .param("user", eperson.getEmail())
                                .param("password", password))
                   // should result in a 403 error as Spring Security returns that for untrusted origins
                   .andExpect(status().isForbidden());
    }

    @Test
    public void testReuseTokenWithDifferentIP() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/status"))

                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.type", is("status")));

        // Verify a different IP address (behind a proxy, i.e. X-FORWARDED-FOR)
        // is able to authenticate with same token
        // NOTE: We allow tokens to be used across several IPs to support environments where your IP is not static.
        // Also keep in mind that if a token is used from an untrusted Origin, it will be blocked (see prior test).
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

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Reviewers")));

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());
        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                                    .with(ip("123.123.123.123")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous", "Administrator")));

        //Simulate that a new shibboleth authentication has happened from another IP
        token = getClient().perform(post("/api/authn/login")
                .with(ip("234.234.234.234"))
                .header("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        getClient(token).perform(get("/api/authn/status").param("projection", "full")
                .with(ip("234.234.234.234")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.okay", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                .andExpect(jsonPath("$.type", is("status")))
                .andExpect(jsonPath("$._links.eperson.href", startsWith(REST_SERVER_URL)))
                .andExpect(jsonPath("$._embedded.eperson",
                        EPersonMatcher.matchEPersonWithGroups(eperson.getEmail(), "Anonymous")));

        getClient(token).perform(
                get("/api/authz/authorizations/search/object")
                        .param("embed", "feature")
                        .param("feature", feature)
                        .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.page.totalElements", is(0)))
                        .andExpect(jsonPath("$._embedded").doesNotExist());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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
                        .andExpect(jsonPath("$.authenticationMethod", is("password")))
                        .andExpect(jsonPath("$.type", is("status")));

        getClient(token).perform(get("/api/authz/authorizations/" + authorization.getID()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$", Matchers.is(
                                AuthorizationMatcher.matchAuthorization(authorization))));
        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.authenticationMethod").doesNotExist())
                        .andExpect(jsonPath("$.type", is("status")));

        //Simulate that a shibboleth authentication has happened
        token = getClient().perform(post("/api/authn/login")
                .requestAttr("SHIB-MAIL", eperson.getEmail())
                .requestAttr("SHIB-SCOPED-AFFILIATION", "faculty;staff"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        //Check if we have a valid token
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")))
                        .andExpect(jsonPath("$.type", is("status")));

        //Logout
        getClient(token).perform(post("/api/authn/logout"))
                        .andExpect(status().isNoContent());

        //Check if we are actually logged out (again)
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.okay", is(true)))
                        .andExpect(jsonPath("$.authenticated", is(false)))
                        .andExpect(jsonPath("$.authenticationMethod").doesNotExist())
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
            .andReturn().getResponse().getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

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

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testShortLivedTokenUsingGet() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify the main session salt doesn't change
        String salt = eperson.getSessionSalt();

        getClient(token).perform(
            get("/api/authn/shortlivedtokens")
                .with(ip(TRUSTED_IP))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token", notNullValue()))
            .andExpect(jsonPath("$.type", is("shortlivedtoken")))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString("/api/authn/shortlivedtokens")))
            // Verify generating short-lived token doesn't change our CSRF token
            // (so, neither the CSRF cookie nor header are sent back)
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        assertEquals(salt, eperson.getSessionSalt());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testShortLivedTokenUsingGetFromUntrustedIpShould403() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(
            get("/api/authn/shortlivedtokens")
                .with(ip(UNTRUSTED_IP))
        )
            .andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testShortLivedTokenUsingGetFromUntrustedIpWithForwardHeaderShould403() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(
            get("/api/authn/shortlivedtokens")
                .with(ip(UNTRUSTED_IP))
                .header("X-Forwarded-For", TRUSTED_IP) // this should not affect the test result
        )
            .andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
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

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }


    @Test
    public void testShortLivedTokenNotAuthenticated() throws Exception {
        getClient().perform(post("/api/authn/shortlivedtokens"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testShortLivedTokenNotAuthenticatedUsingGet() throws Exception {
        getClient().perform(
            get("/api/authn/shortlivedtokens")
                .with(ip(TRUSTED_IP))
        )
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testShortLivedTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        String token = getAuthToken(eperson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);

        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isOk());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());

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

        String token = getAuthToken(testEPerson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testLoginTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();

        String loginToken = getAuthToken(eperson.getEmail(), password);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + loginToken))
            .andExpect(status().isUnauthorized());

        // Logout, invalidating token
        getClient(loginToken).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testExpiredShortLivedTokenToDownloadBitstream() throws Exception {
        Bitstream bitstream = createPrivateBitstream();
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");
        String token = getAuthToken(eperson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);
        Thread.sleep(1);
        getClient().perform(get("/api/core/bitstreams/" + bitstream.getID()
                + "/content?authentication-token=" + shortLivedToken))
            .andExpect(status().isUnauthorized());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testShortLivedAndLoginTokenSeparation() throws Exception {
        configurationService.setProperty("jwt.shortLived.token.expiration", "1");

        String token = getAuthToken(eperson.getEmail(), password);
        Thread.sleep(2);
        getClient(token).perform(get("/api/authn/status").param("projection", "full"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", is(true)));

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    // TODO: fix the exception. For now we want to verify a short lived token can't be used to login
    @Test(expected = Exception.class)
    public void testLoginWithShortLivedToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);

        getClient().perform(post("/api/authn/login?authentication-token=" + shortLivedToken))
            .andExpect(status().isInternalServerError());
        // TODO: This internal server error needs to be fixed. This should actually produce a forbidden status
        //.andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGenerateShortLivedTokenWithShortLivedToken() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);

        getClient().perform(post("/api/authn/shortlivedtokens?authentication-token=" + shortLivedToken))
            .andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGenerateShortLivedTokenWithShortLivedTokenUsingGet() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        String shortLivedToken = getShortLivedToken(token);

        getClient().perform(
            get("/api/authn/shortlivedtokens?authentication-token=" + shortLivedToken)
                .with(ip(TRUSTED_IP))
        )
            .andExpect(status().isForbidden());

        // Logout, invalidating token
        getClient(token).perform(post("/api/authn/logout"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testStatusOrcidAuthenticatedWithCookie() throws Exception {

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", ORCID_ONLY);

        String uiURL = configurationService.getProperty("dspace.ui.url");

        context.turnOffAuthorisationSystem();

        String orcid = "0000-1111-2222-3333";
        String code = "123456";
        String orcidAccessToken = "c41e37e5-c2de-4177-91d6-ed9e9d1f31bf";

        EPersonBuilder.createEPerson(context)
            .withEmail("test@email.it")
            .withNetId(orcid)
            .withNameInMetadata("Test", "User")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        OrcidClient orcidClientMock = mock(OrcidClient.class);
        when(orcidClientMock.getAccessToken(code)).thenReturn(buildOrcidTokenResponse(orcid, orcidAccessToken));

        OrcidClient originalOrcidClient = orcidAuthentication.getOrcidClient();
        orcidAuthentication.setOrcidClient(orcidClientMock);

        Cookie authCookie = null;

        try {

            authCookie = getClient().perform(get("/api/" + AuthnRest.CATEGORY + "/orcid")
                .param("redirectUrl", uiURL)
                .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(uiURL))
                .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
                .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"))
                .andExpect(cookie().exists(AUTHORIZATION_COOKIE))
                .andReturn().getResponse().getCookie(AUTHORIZATION_COOKIE);

        } finally {
            orcidAuthentication.setOrcidClient(originalOrcidClient);
        }

        assertNotNull(authCookie);
        String token = authCookie.getValue();

        getClient().perform(get("/api/authn/status").header("Origin", uiURL)
            .secure(true)
            .cookie(authCookie))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.authenticationMethod", is("orcid")))
            .andExpect(jsonPath("$.type", is("status")))
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"));

        String headerToken = getClient().perform(post("/api/authn/login").header("Origin", uiURL)
            .secure(true)
            .cookie(authCookie))
            .andExpect(status().isOk())
            .andExpect(cookie().value(AUTHORIZATION_COOKIE, ""))
            .andExpect(header().exists(AUTHORIZATION_HEADER))
            .andExpect(cookie().exists("DSPACE-XSRF-COOKIE"))
            .andExpect(header().exists("DSPACE-XSRF-TOKEN"))
            .andReturn().getResponse()
            .getHeader(AUTHORIZATION_HEADER).replace(AUTHORIZATION_TYPE, "");

        assertTrue("Check tokens " + token + " and " + headerToken + " have same claims",
            tokenClaimsEqual(token, headerToken));

        getClient(headerToken).perform(get("/api/authn/status").header("Origin", uiURL))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$.okay", is(true)))
            .andExpect(jsonPath("$.authenticated", is(true)))
            .andExpect(jsonPath("$.authenticationMethod", is("orcid")))
            .andExpect(jsonPath("$.type", is("status")));

        getClient(headerToken).perform(post("/api/authn/logout").header("Origin", uiURL))
            .andExpect(status().isNoContent());
    }

    @Test
    public void testOrcidLoginURL() throws Exception {

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", ORCID_ONLY);

        String originalClientId = orcidConfiguration.getClientId();
        orcidConfiguration.setClientId("CLIENT-ID");

        try {

            getClient().perform(post("/api/authn/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                    "orcid realm=\"DSpace REST API\", " +
                        "location=\"https://sandbox.orcid.org/oauth/authorize?client_id=CLIENT-ID&response_type=code"
                        + "&scope=/authenticate+/read-limited+/activities/update+/person/update&redirect_uri"
                        + "=http%3A%2F%2Flocalhost%2Fapi%2Fauthn%2Forcid\""));

        } finally {
            orcidConfiguration.setClientId(originalClientId);
        }
    }

    // Get a short-lived token based on an active login token
    private String getShortLivedToken(String loginToken) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        MvcResult mvcResult = getClient(loginToken).perform(post("/api/authn/shortlivedtokens"))
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

    /**
     * Check if the claims (except for expiration date) are equal between two JWTs.
     * Expiration date (exp) claim is ignored as it includes a timestamp and therefore changes every second.
     * So, this method checks to ensure token equality by comparing all other claims.
     *
     * @param token1 first token
     * @param token2 second token
     * @return True if tokens are identical or have the same claims (ignoring "exp"). False otherwise.
     */
    private boolean tokenClaimsEqual(String token1, String token2) {
        // First check for exact match tokens. These are guaranteed to have equal claims.
        if (token1.equals(token2)) {
            return true;
        }

        // Now parse the two tokens and compare the claims in each
        try {
            SignedJWT jwt1 = SignedJWT.parse(token1);
            SignedJWT jwt2 = SignedJWT.parse(token2);
            JWTClaimsSet jwt1ClaimsSet = jwt1.getJWTClaimsSet();
            JWTClaimsSet jwt2ClaimsSet = jwt2.getJWTClaimsSet();

            Map<String,Object> jwt1Claims = jwt1ClaimsSet.getClaims();
            for (String claim: jwt1Claims.keySet()) {
                // Ignore the "exp" (expiration date) claim, as it includes a timestamp and changes every second
                if (claim.equals("exp")) {
                    continue;
                }
                // If one other claim is not equal, return false
                if (!jwt1ClaimsSet.getClaim(claim).equals(jwt2ClaimsSet.getClaim(claim))) {
                    return false;
                }
            }
            // all claims (except "exp") are equal!
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private OrcidTokenResponseDTO buildOrcidTokenResponse(String orcid, String accessToken) {
        OrcidTokenResponseDTO token = new OrcidTokenResponseDTO();
        token.setAccessToken(accessToken);
        token.setOrcid(orcid);
        token.setTokenType("Bearer");
        token.setName("Test User");
        token.setScope(String.join(" ", new String[] { "FirstScope", "SecondScope" }));
        return token;
    }
}

