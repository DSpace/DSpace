/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.clarin.ClarinShibbolethLoginFilter.MISSING_HEADERS_FROM_IDP;
import static org.dspace.app.rest.security.clarin.ClarinShibbolethLoginFilter.USER_WITHOUT_EMAIL_EXCEPTION;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.clarin.ClarinVerificationToken;
import org.dspace.content.service.clarin.ClarinVerificationTokenService;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
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
                .withNetId("123456789")
                .build();
        context.restoreAuthSystemState();
    }

    /**
     * Test the IdP hasn't sent the `Shib-Identity-Provider` or `SHIB-NETID` header.
     */
    @Test
    public void shouldReturnMissingHeadersFromIdpExceptionBecauseOfMissingIdp() throws Exception {
        String netId = "123456";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-NETID", netId))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(MISSING_HEADERS_FROM_IDP));
    }

    /**
     * Test the IdP hasn't sent the `Shib-Identity-Provider` or `SHIB-NETID` header.
     */
    @Test
    public void shouldReturnMissingHeadersFromIdpExceptionBecauseOfMissingNetId() throws Exception {
        String idp = "Test Idp";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(MISSING_HEADERS_FROM_IDP));
    }

    /**
     * Test:
     * The IdP hasn't sent the `SHIB-EMAIL` header.
     * The request headers passed by IdP are stored into the `verification_token` table the `shib_headers` column.
     * The user is redirected to the page when he must fill his email.
     */
    @Test
    public void shouldReturnUserWithoutEmailException() throws Exception {
        String netId = "123456";
        String idp = "Test Idp";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-NETID", netId)
                        .header("Shib-Identity-Provider", idp))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(USER_WITHOUT_EMAIL_EXCEPTION + "," + netId));
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
        String netId = "123456";
        String email = "test@mail.epic";
        String firstname = "Test";
        String lastname = "Buddy";
        String idp = "Test Idp";

        // Try to authenticate but the Shibboleth doesn't send the email in the header, so the user won't be registered
        // but the user will be redirected to the page where he will fill in the user email.
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId)
                        .header("SHIB-GIVENNAME", firstname)
                        .header("SHIB-SURNAME", lastname))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(USER_WITHOUT_EMAIL_EXCEPTION + "," + netId));

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
        EPerson ePerson = ePersonService.findByNetid(context, netId);
        assertTrue(Objects.nonNull(ePerson));
        assertEquals(ePerson.getEmail(), email);
        assertEquals(ePerson.getFirstName(), firstname);
        assertEquals(ePerson.getLastName(), lastname);

        // The user is registered now log him
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId)
                        .header("verification-token", clarinVerificationToken.getToken()))
                .andExpect(status().isOk());

        // Try to sign in the user by the email if the eperson exist
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId)
                        .header("SHIB-GIVENNAME", firstname)
                        .header("SHIB-SURNAME", lastname)
                        .header("SHIB-MAIL", email))
                .andExpect(status().isFound());

        // Try to sign in the user by the netid if the eperson exist
        getClient().perform(get("/api/authn/shibboleth")
                        .header("Shib-Identity-Provider", idp)
                        .header("SHIB-NETID", netId)
                        .header("SHIB-GIVENNAME", firstname)
                        .header("SHIB-SURNAME", lastname))
                .andExpect(status().isFound());
    }

    // This test is copied from the `ShibbolethLoginFilterIT` and modified following the Clarin updates.
    @Test
    public void testRedirectToGivenTrustedUrl() throws Exception {
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:8080/server/api/authn/status"))
                .andReturn().getResponse().getHeader("Authorization");

        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

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
                    .header("Shib-Identity-Provider", "Test idp")
                    .header("SHIB-NETID", clarinEperson.getNetid()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");


        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

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
                    .header("Shib-Identity-Provider", "Test idp")
                    .header("SHIB-NETID", clarinEperson.getNetid()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");


        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

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
    public void testRedirectToAnotherGivenTrustedUrl() throws Exception {
        getClient().perform(get("/api/authn/shibboleth")
                        .param("redirectUrl", "http://anotherdspacehost:4000/home")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid()))
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
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid()))
                .andExpect(status().isBadRequest());
    }
}
