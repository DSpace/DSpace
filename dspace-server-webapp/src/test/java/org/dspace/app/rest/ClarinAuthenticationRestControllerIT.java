/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.ParseException;
import java.util.Map;
import javax.servlet.http.Cookie;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.impl.CanChangePasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.EPersonBuilder;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Override of the `AuthenticationRestControllerIT` class because of Clarin Shibboleth Authentication updates.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk).
 */
public class ClarinAuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.clarin.ClarinShibAuthentication"};
    private EPersonRest ePersonRest;
    private final String feature = CanChangePasswordFeature.NAME;
    private Authorization authorization;
    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private Utils utils;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Before
    public void setup() throws Exception {
        super.setUp();

        AuthorizationFeature canChangePasswordFeature = authorizationFeatureService.find(CanChangePasswordFeature.NAME);
        ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);
        authorization = new Authorization(eperson, canChangePasswordFeature, ePersonRest);

        // Default all tests to Password Authentication only
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);
    }

    // This test is copied from the `AuthenticationRestControllerIT` and modified following the Clarin updates.
    @Test
    public void testStatusShibAuthenticatedWithCookie() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson clarinEperson = EPersonBuilder.createEPerson(context)
                .withCanLogin(false)
                .withEmail("clarin@email.com")
                .withNameInMetadata("first", "last")
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .withNetId("123456789")
                .build();
        context.restoreAuthSystemState();

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
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid())
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
}
