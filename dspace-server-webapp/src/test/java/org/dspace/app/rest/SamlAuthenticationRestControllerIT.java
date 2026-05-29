/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.text.ParseException;
import java.util.List;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.Cookie;
import org.dspace.app.rest.security.jwt.EPersonClaimProvider;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.saml2.DSpaceRelyingPartyRegistrationRepository;
import org.dspace.services.ConfigurationService;
import org.dspace.util.UUIDUtils;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringEndsWith;
import org.hamcrest.core.StringStartsWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

public class SamlAuthenticationRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DSpaceRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @Autowired
    private EPersonService ePersonService;

    private EPerson testUser;

    @Before
    public void beforeEach() throws Exception {
        testUser = null;

        // Enable SAML authentication.

        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod",
            List.of("org.dspace.authenticate.SamlAuthentication", "org.dspace.authenticate.PasswordAuthentication"));

        // Enable SAML autoregistration.

        configurationService.setProperty("authentication-saml.autoregister", true);

        // Configure a SAML relying party with ID testrp.

        configurationService.setProperty("saml-relying-party.testrp.asserting-party.entity-id",
            "urn:idp.example.com");

        configurationService.setProperty("saml-relying-party.testrp.asserting-party.single-sign-on.url",
            "http://idp.example.com/samlp");

        configurationService.setProperty("saml-relying-party.testrp.asserting-party.single-sign-on.binding",
            "REDIRECT");

        configurationService.setProperty("saml-relying-party.testrp.signing.credentials.0.private-key-location",
            "classpath:org/dspace/app/rest/testrp-rp-private.key");

        configurationService.setProperty("saml-relying-party.testrp.signing.credentials.0.certificate-location",
            "classpath:org/dspace/app/rest/testrp-rp-certificate.crt");

        relyingPartyRegistrationRepository.reload();
    }

    @After
    public void afterEach() throws Exception {
        if (testUser != null) {
            EPersonBuilder.deleteEPerson(testUser.getID());
        }
    }

    @Test
    public void testSamlMetadata() throws Exception {
        // Retrieve the relying party metadata. This should contain the URL of the assertion consumer endpoint (so the
        // asserting party knows where to send the identity assertion after a successful login), and the signing
        // certificate (so the asserting party can verify signatures on signed login requests).

        getClient().perform(get("/saml2/service-provider-metadata/testrp"))
            .andExpect(status().is2xxSuccessful())
            .andExpect(header().string("content-type", new StringStartsWith("application/samlmetadata+xml")))
            .andExpect(
                xpath("/EntityDescriptor/SPSSODescriptor/AssertionConsumerService/@Location")
                    .string("http://localhost/saml2/assertion-consumer/testrp"))
            .andExpect(
                xpath(
                    "/EntityDescriptor/SPSSODescriptor/KeyDescriptor[@use='signing']/KeyInfo/X509Data/X509Certificate")
                    .string(new AllOf<String>(
                        new StringStartsWith("MIICgTCCAeoCCQCuVzyqFgMSyDAN"),
                        new StringEndsWith("RZ/nbTJ7VTeZOSyRoVn5XHhpuJ0B"))))
            .andReturn();
    }

    @Test
    public void testSamlInitiateLogin() throws Exception {
        // Initiate a login using the relying party. The relying party has been configured with an asserting party that
        // has a REDIRECT single sign-on binding, so this should result in a redirect to the assserting party's single
        // sign-on URL, with a SAMLRequest query parameter.

        getClient().perform(get("/saml2/authenticate/testrp"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("location",
                new StringStartsWith("http://idp.example.com/samlp?SAMLRequest=")))
            .andReturn();
    }

    @Test
    public void testSamlAttributesReceivedForExistingUser() throws Exception {
        // After a successful login, the asserting party will cause the user's browser to post an identity assertion to
        // the relying party's assertion consumer endpoint. Attributes from the assertion are mapped into request
        // attributes, and the request is forwarded to the DSpace SAML authentication endpoint. If an existing DSpace
        // user can be located from the SAML attributes, that user should be logged in.

        context.turnOffAuthorisationSystem();

        testUser = EPersonBuilder.createEPerson(context)
            .withEmail("alyssa@dspace.org")
            .withNameInMetadata("Alyssa", "Hacker")
            .withCanLogin(true)
            .build();

        context.restoreAuthSystemState();

        MvcResult mvcResult = getClient().perform(get("/api/authn/saml")
            .requestAttr("org.dspace.saml.EMAIL", "alyssa@dspace.org"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            // CSRF cookie and token header should not be present on SAML login responses, because the request came
            // directly from the browser, not from JavaScript. The cookie would be stored by the browser, but nothing
            // would handle the token header to keep them in sync.
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"))
            .andReturn();

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);

        assertNotNull(ePersonId);
        assertEquals(testUser.getID().toString(), ePersonId);
    }

    @Test
    public void testSamlAttributesReceivedForNonexistentUser() throws Exception {
        // If an existing DSpace user cannot be located from the SAML attributes, that user should be autoregistered
        // and logged in, if autoregister is enabled in configuration.

        MvcResult mvcResult = getClient().perform(get("/api/authn/saml")
            .requestAttr("org.dspace.saml.NAME_ID", "001")
            .requestAttr("org.dspace.saml.EMAIL", "ben@dspace.org")
            .requestAttr("org.dspace.saml.GIVEN_NAME", "Ben")
            .requestAttr("org.dspace.saml.SURNAME", "Bitdiddle"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(configurationService.getProperty("dspace.ui.url")))
            .andExpect(cookie().exists("Authorization-cookie"))
            // CSRF cookie and token header should not be present on SAML login responses, because the request came
            // directly from the browser, not from JavaScript. The cookie would be stored by the browser, but nothing
            // would handle the token header to keep them in sync.
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"))
            .andReturn();

        String ePersonId = getEPersonIdFromAuthorizationCookie(mvcResult);

        testUser = ePersonService.find(context, UUIDUtils.fromString(ePersonId));

        assertNotNull(testUser);
        assertEquals("001", testUser.getNetid());
        assertEquals("ben@dspace.org", testUser.getEmail());
        assertEquals("Ben", testUser.getFirstName());
        assertEquals("Bitdiddle", testUser.getLastName());
    }

    @Test
    public void testSamlAttributesReceivedForNonexistentUserWithAutoregisterDisabled() throws Exception {
        // If an existing DSpace user cannot be located from the SAML attributes, log in should fail if autoregister is
        // disabled in configuration.

        configurationService.setProperty("authentication-saml.autoregister", false);

        getClient().perform(get("/api/authn/saml")
            .requestAttr("org.dspace.saml.NAME_ID", "001")
            .requestAttr("org.dspace.saml.EMAIL", "ben@dspace.org")
            .requestAttr("org.dspace.saml.GIVEN_NAME", "Ben")
            .requestAttr("org.dspace.saml.SURNAME", "Bitdiddle"))
            .andExpect(status().isUnauthorized())
            .andExpect(cookie().doesNotExist("Authorization-cookie"))
            .andExpect(header().exists("WWW-Authenticate"))
            // CSRF cookie and token header should not be present on SAML login responses, because the request came
            // directly from the browser, not from JavaScript. The cookie would be stored by the browser, but nothing
            // would handle the token header to keep them in sync.
            .andExpect(cookie().doesNotExist("DSPACE-XSRF-COOKIE"))
            .andExpect(header().doesNotExist("DSPACE-XSRF-TOKEN"))
            .andReturn();
    }

    private String getEPersonIdFromAuthorizationCookie(MvcResult mvcResult) throws ParseException, JOSEException {
        Cookie authorizationCookie = mvcResult.getResponse().getCookie("Authorization-cookie");
        SignedJWT jwt = SignedJWT.parse(authorizationCookie.getValue());

        return (String) jwt.getJWTClaimsSet().getClaim(EPersonClaimProvider.EPERSON_ID);
    }
}
