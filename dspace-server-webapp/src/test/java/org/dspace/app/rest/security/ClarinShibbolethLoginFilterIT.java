/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.ShibbolethLoginFilterIT.PASS_ONLY;
import static org.dspace.app.rest.security.clarin.ClarinShibbolethLoginFilter.VERIFICATION_TOKEN_HEADER;
import static org.dspace.rdf.negotiation.MediaRange.token;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.authorization.impl.CanChangePasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.Util;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The test class for the customized Shibboleth Authentication Process.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk).
 */
public class ClarinShibbolethLoginFilterIT extends AbstractControllerIntegrationTest {

    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.clarin.ClarinShibAuthentication"};
    private static final String NET_ID_EPPN_HEADER = "eppn";
    private static final String NET_ID_PERSISTENT_ID = "persistent-id";
    private static final String NET_ID_TEST_EPERSON = "123456789";
    private static final String IDP_TEST_EPERSON = "Test Idp";
    private static final String KNIHOVNA_KUN_TEST_ZLUTOUCKY = "knihovna Kůň test Žluťoučký";


    private EPersonRest ePersonRest;
    private final String feature = CanChangePasswordFeature.NAME;
    private EPerson clarinEperson;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ClarinVerificationTokenService clarinVerificationTokenService;

    @Autowired
    EPersonService ePersonService;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private Utils utils;


    @Before
    public void setup() throws Exception {
        super.setUp();
        // Add a second trusted host for some tests
        configurationService.setProperty("rest.cors.allowed-origins",
                "${dspace.ui.url}, http://anotherdspacehost:4000");

        // Enable Shibboleth login for all tests
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
        ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);

        context.turnOffAuthorisationSystem();
        clarinEperson = EPersonBuilder.createEPerson(context)
                .withCanLogin(false)
                .withEmail("clarin@email.com")
                .withNameInMetadata("first", "last")
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .withNetId(Util.formatNetId(NET_ID_TEST_EPERSON, IDP_TEST_EPERSON))
                .build();
        context.restoreAuthSystemState();
    }

    @Override
    @After
    public void destroy() throws Exception {
        // Remove the created user manually because some tests are failing
        EPersonBuilder.deleteEPerson(clarinEperson.getID());
        super.destroy();
    }

    /**
     * Test the IdP hasn't sent the `Shib-Identity-Provider` or `SHIB-NETID` header.
     */
    @Test
    public void shouldReturnMissingHeadersFromIdpExceptionBecauseOfMissingIdp() throws Exception {
        String idp = "Test Idp";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:4000/login/missing-headers"));
    }

    /**
     * Test:
     * The IdP hasn't sent the `SHIB-EMAIL` header.
     * The request headers passed by IdP are stored into the `verification_token` table the `shib_headers` column.
     * The user is redirected to the page when he must fill his email.
     */
    // HERE
    @Test
    public void shouldReturnUserWithoutEmailException() throws Exception {
        // Create a new netId because the user shouldn't exist
        String netId  = NET_ID_TEST_EPERSON + 986;
        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-NETID", netId)
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:4000/login/auth-failed?netid=" +
                        Util.formatNetId(netId, IDP_TEST_EPERSON)));
    }

    /**
     * Test the authentication process:
     * 1. The IdP hasn't sent the `SHIB-EMAIL` header and the user is redirected to the page to fill in his email.
     * 2. Test to `ClarinAutoregistrationController.sendEmail` method to send the verification email to the users
     * email.
     * 3. Validate the users verification token and authenticate the user by the verification token. The user is
     * automatically registered and signed in.
     * 4. If the user is registered he is automatically signed in by the NETID which is passed from the IdP.
     * @throws Exception
     */
    @Test
    public void userFillInEmailAndShouldBeRegisteredByVerificationToken() throws Exception {
        String email = "test@mail.epic";
        String netId = email;
        String idp = "Test Idp";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:4000/login/auth-failed?netid=" +
                        Util.formatNetId(netId, idp)));

        // Send the email with the verification token.
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/autoregistration?netid=" + netId + "&email=" + email)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // Load the created verification token.
        ClarinVerificationToken clarinVerificationToken = clarinVerificationTokenService.findByNetID(context, netId);
        assertTrue(Objects.nonNull(clarinVerificationToken));

        // Register the user by the verification token.
        getClient(tokenAdmin).perform(get("/api/autoregistration?verification-token=" +
                        clarinVerificationToken.getToken())
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk());

        // Check if was created a user with such email and netid.
        EPerson ePerson = checkUserWasCreated(netId, idp, email, null);

        // The user is registered now log him
        getClient().perform(post("/api/authn/shibboleth")
                        .header(VERIFICATION_TOKEN_HEADER, clarinVerificationToken.getToken()))
                .andExpect(status().isOk());

        // Try to sign in the user by the email if the eperson exist
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId)
                        .header("SHIB-MAIL", email))
                .andExpect(status().isFound());

        // Try to sign in the user by the netid if the eperson exist
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId))
                .andExpect(status().isFound());

        // Delete created eperson - clean after the test
        deleteShibbolethUser(ePerson);
    }

    @Test
    public void testShouldReturnDuplicateUserError() throws Exception {
        String email = "test@mail.epic";
        String netId = email;

        String differentIdP = "Different IdP";

        // login through shibboleth
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", email)
                        .header("SHIB-NETID", netId)
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        // Check if was created a user with such email and netid.
        EPerson ePerson = ePersonService.findByNetid(context, Util.formatNetId(netId, IDP_TEST_EPERSON));
        assertTrue(Objects.nonNull(ePerson));
        assertEquals(ePerson.getEmail(), email);
        assertEquals(ePerson.getNetid(), Util.formatNetId(netId, IDP_TEST_EPERSON));

        // Try to login with the same email, but from the different IdP, the user should be redirected to the
        // duplicate error page
        getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", email)
                        .header("SHIB-NETID", netId)
                        .header("Shib-Identity-Provider", differentIdP))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000/login/duplicate-user?email=" + email))
                .andReturn().getResponse().getHeader("Authorization");

        // Delete created eperson - clean after the test
        EPersonBuilder.deleteEPerson(ePerson.getID());
    }

    // Login with email without netid, but the user with such email already exists and it has assigned netid.
    @Test
    public void testShouldReturnDuplicateUserErrorLoginWithoutNetId() throws Exception {
        String email = "test@email.sk";
        String netId = email;

        // login through shibboleth
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", email)
                        .header("SHIB-NETID", netId)
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        // Should not login because the user with such email already exists
        getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", email)
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000/login/duplicate-user?email=" + email))
                .andReturn().getResponse().getHeader("Authorization");

        // Check if was created a user with such email and netid.
        EPerson ePerson = checkUserWasCreated(netId, IDP_TEST_EPERSON, email, null);
        deleteShibbolethUser(ePerson);
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectToGivenTrustedUrl() throws Exception {
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:8080/server/api/authn/status"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        getClient(token).perform(
                        get("/api/authz/authorizations/search/object")
                                .param("embed", "feature")
                                .param("feature", feature)
                                .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void patchPassword() throws Exception {
        String newPassword = "newpassword";

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/password", newPassword);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        // login through shibboleth
        String token = getClient().perform(get("/api/authn/shibboleth")
                    .header("SHIB-MAIL", clarinEperson.getEmail())
                    .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                    .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + clarinEperson.getID())
                        .content(patchBody)
                        .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isForbidden());

    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectToDefaultDspaceUrl() throws Exception {
        // NOTE: The initial call to /shibboleth comes *from* an external Shibboleth site. So, it is always
        // unauthenticated, but it must include some expected SHIB attributes.
        // SHIB-MAIL attribute is the default email header sent from Shibboleth after a successful login.
        // In this test we are simply mocking that behavior by setting it to an existing EPerson.
        String token = getClient().perform(get("/api/authn/shibboleth")
                    .header("SHIB-MAIL", clarinEperson.getEmail())
                    .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                    .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        getClient(token).perform(
                        get("/api/authz/authorizations/search/object")
                                .param("embed", "feature")
                                .param("feature", feature)
                                .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded").doesNotExist());

    }

    // This test is copied from the `AuthenticationRestControllerIT` and modified following the Clarin updates.
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
                .andExpect(status().isFound())
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

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testNoRedirectIfInvalidShibAttributes() throws Exception {
        // In this request, we use a SHIB-MAIL attribute which does NOT match an EPerson.
        getClient().perform(get("/api/authn/shibboleth")
                        .requestAttr("SHIB-MAIL", "not-an-eperson@example.com"))
                .andExpect(status().isFound());
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testNoRedirectIfShibbolethDisabled() throws Exception {
        // Enable Password authentication ONLY
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        // Test redirecting to a trusted URL (same as previous test).
        // This time we should be unauthorized as Shibboleth is disabled.
        getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                        .requestAttr("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isFound());
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectRequiresShibAttributes2() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify this endpoint also doesn't work using a regular auth token (again if SHIB-* attributes missing)
        getClient(token).perform(get("/api/authn/shibboleth"))
                .andExpect(status().isFound());
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectRequiresShibAttributes() throws Exception {
        // Verify this endpoint doesn't work if no SHIB-* attributes are set
        getClient().perform(get("/api/authn/shibboleth"))
                .andExpect(status().isFound());
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectToAnotherGivenTrustedUrl() throws Exception {
        getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://anotherdspacehost:4000/home")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://anotherdspacehost:4000/home"));
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectToGivenUntrustedUrl() throws Exception {
        // Now attempt to redirect to a URL that is NOT trusted (i.e. not in 'rest.cors.allowed-origins').

        // Should result in a 400 error.
        getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://dspace.org")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testISOShibHeaders() throws Exception {
        String testMail = "test@email.edu";
        String testIdp = IDP_TEST_EPERSON + "test";
        String testNetId = NET_ID_TEST_EPERSON + "000";
        // NOTE: The initial call to /shibboleth comes *from* an external Shibboleth site. So, it is always
        // unauthenticated, but it must include some expected SHIB attributes.
        // SHIB-MAIL attribute is the default email header sent from Shibboleth after a successful login.
        // In this test we are simply mocking that behavior by setting it to an existing EPerson.
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", testMail)
                        .header("Shib-Identity-Provider", testIdp)
                        .header("SHIB-NETID", testNetId)
                        .header("SHIB-GIVENNAME", "knihovna KÅ¯Å\u0088 test Å½luÅ¥ouÄ\u008DkÃ½"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);
        // Check if was created a user with such email and netid.
        EPerson ePerson = checkUserWasCreated(testNetId, testIdp, testMail, KNIHOVNA_KUN_TEST_ZLUTOUCKY);
        deleteShibbolethUser(ePerson);
    }

    @Test
    public void testUTF8ShibHeaders() throws Exception {
        String testMail = "test@email.edu";
        String testIdp = IDP_TEST_EPERSON + "test";
        String testNetId = NET_ID_TEST_EPERSON + "000";
        // NOTE: The initial call to /shibboleth comes *from* an external Shibboleth site. So, it is always
        // unauthenticated, but it must include some expected SHIB attributes.
        // SHIB-MAIL attribute is the default email header sent from Shibboleth after a successful login.
        // In this test we are simply mocking that behavior by setting it to an existing EPerson.
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", testMail)
                        .header("Shib-Identity-Provider", testIdp)
                        .header("SHIB-NETID", testNetId)
                        .header("SHIB-GIVENNAME", KNIHOVNA_KUN_TEST_ZLUTOUCKY))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);
        // Check if was created a user with such email and netid.
        EPerson ePerson = checkUserWasCreated(testNetId, testIdp, testMail, KNIHOVNA_KUN_TEST_ZLUTOUCKY);
        deleteShibbolethUser(ePerson);
    }

    @Test
    public void testRedirectToMissingHeadersWithRedirectUrlParam() throws Exception {
        String expectedMissingHeadersUrl = configurationService.getProperty("dspace.ui.url") + "/login/missing-headers";

        getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("SHIB-NETID", NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedMissingHeadersUrl));
    }

    // eppn is set
    @Test
    public void testSuccessFullLoginEppnNetId() throws Exception {
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header(NET_ID_EPPN_HEADER, NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);

        EPerson ePerson = checkUserWasCreated(NET_ID_TEST_EPERSON, IDP_TEST_EPERSON, clarinEperson.getEmail(), null);
        deleteShibbolethUser(ePerson);
    }

    // persistent-id is set
    @Test
    public void testSuccessFullLoginPersistentIdNetId() throws Exception {
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header(NET_ID_PERSISTENT_ID, NET_ID_TEST_EPERSON))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);
        EPerson ePerson = checkUserWasCreated(NET_ID_TEST_EPERSON, IDP_TEST_EPERSON, clarinEperson.getEmail(), null);
        deleteShibbolethUser(ePerson);
    }

    @Test
    public void testSuccessFullLoginWithTwoEmails() throws Exception {
        String firstEmail = "efg@test.edu";
        String secondEmail = "abc@test.edu";
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header("SHIB-MAIL", firstEmail + ";" + secondEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(token);
        // Find the user by the second email
        EPerson ePerson = checkUserWasCreated(null, IDP_TEST_EPERSON, secondEmail, null);
        assertTrue(Objects.nonNull(ePerson));
        deleteShibbolethUser(ePerson);
    }

    // The user has changed the email. But that email is already used by another user.
    @Test
    public void testDuplicateEmailError() throws Exception {
        String userWithEppnEmail = "user@eppn.sk";
        String customEppn = "custom eppn";

        // Create a user with netid and email
        String tokenEppnUser = getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header(NET_ID_PERSISTENT_ID, customEppn)
                        .header("SHIB-MAIL", userWithEppnEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        checkUserIsSignedIn(tokenEppnUser);

        // Try to update an email of existing user - the email is already used by another user - the user should be
        // redirected to the login page
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", IDP_TEST_EPERSON)
                        .header(NET_ID_PERSISTENT_ID, NET_ID_TEST_EPERSON)
                        .header("SHIB-MAIL", userWithEppnEmail))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000/login/duplicate-user?email=" + userWithEppnEmail));

        // Check if was created a user with such email and netid.
        EPerson ePerson = checkUserWasCreated(customEppn, IDP_TEST_EPERSON, userWithEppnEmail, null);
        // Delete created eperson - clean after the test
        deleteShibbolethUser(ePerson);
    }

    private EPerson checkUserWasCreated(String netIdValue, String idpValue, String email, String name)
            throws SQLException {
        // Check if was created a user with such email and netid.
        EPerson ePerson = null;
        if (netIdValue != null) {
            ePerson = ePersonService.findByNetid(context, Util.formatNetId(netIdValue, idpValue));
        } else {
            ePerson = ePersonService.findByEmail(context, email);
        }
        assertTrue(Objects.nonNull(ePerson));
        if (email != null) {
            assertEquals(ePerson.getEmail(), email);
        }

        if (name != null) {
            assertEquals(ePerson.getFirstName(), name);
        }
        return ePerson;
    }

    private void checkUserIsSignedIn(String token) throws Exception {
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));
    }


    private void deleteShibbolethUser(EPerson ePerson) throws Exception {
        EPersonBuilder.deleteEPerson(ePerson.getID());

        // Check it was correctly deleted
        getClient(token).perform(
                        get("/api/authz/authorizations/search/object")
                                .param("embed", "feature")
                                .param("feature", feature)
                                .param("uri", utils.linkToSingleResource(ePersonRest, "self").getHref()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", is(0)))
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
