/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.impl.CanChangePasswordFeature;
import org.dspace.app.rest.converter.EPersonConverter;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.builder.EPersonBuilder;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests that cover ShibbolethLoginFilter behavior (especially around redirects)
 *
 * @author Giuseppe Digilio (giuseppe dot digilio at 4science dot it)
 */
public class ShibbolethLoginFilterIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private EPersonConverter ePersonConverter;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private Utils utils;

    public static final String[] PASS_ONLY = {"org.dspace.authenticate.PasswordAuthentication"};
    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.ShibAuthentication"};

    private EPersonRest ePersonRest;
    private final String feature = CanChangePasswordFeature.NAME;

    @Before
    public void setup() throws Exception {
        super.setUp();

        AuthorizationFeature canChangePasswordFeature = authorizationFeatureService.find(CanChangePasswordFeature.NAME);
        ePersonRest = ePersonConverter.convert(eperson, DefaultProjection.DEFAULT);

        // Add a second trusted host for some tests
        configurationService.setProperty("rest.cors.allowed-origins",
                "${dspace.ui.url}, http://anotherdspacehost:4000");

        // Enable Shibboleth login for all tests
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);
    }

    @Test
    public void testRedirectToDefaultDspaceUrl() throws Exception {
        // NOTE: The initial call to /shibboleth comes *from* an external Shibboleth site. So, it is always
        // unauthenticated, but it must include some expected SHIB attributes.
        // SHIB-MAIL attribute is the default email header sent from Shibboleth after a successful login.
        // In this test we are simply mocking that behavior by setting it to an existing EPerson.
        String token = getClient().perform(get("/api/authn/shibboleth").requestAttr("SHIB-MAIL", eperson.getEmail()))
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

    @Test
    public void testRedirectToGivenTrustedUrl() throws Exception {
        String token = getClient().perform(get("/api/authn/shibboleth")
                      .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                      .requestAttr("SHIB-MAIL", eperson.getEmail()))
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

    @Test
    public void testNoRedirectIfShibbolethDisabled() throws Exception {
        // Enable Password authentication ONLY
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", PASS_ONLY);

        // Test redirecting to a trusted URL (same as previous test).
        // This time we should be unauthorized as Shibboleth is disabled.
        getClient().perform(get("/api/authn/shibboleth")
                       .param("redirectUrl", "http://localhost:8080/server/api/authn/status")
                       .requestAttr("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRedirectToAnotherGivenTrustedUrl() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient().perform(get("/api/authn/shibboleth")
                       .param("redirectUrl", "http://anotherdspacehost:4000/home")
                       .requestAttr("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://anotherdspacehost:4000/home"));
    }

    @Test
    public void testRedirectToGivenUntrustedUrl() throws Exception {
        // Now attempt to redirect to a URL that is NOT trusted (i.e. not in 'rest.cors.allowed-origins').

        // Should result in a 400 error.
        getClient().perform(get("/api/authn/shibboleth")
                       .param("redirectUrl", "http://dspace.org")
                       .requestAttr("SHIB-MAIL", eperson.getEmail()))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testNoRedirectIfInvalidShibAttributes() throws Exception {
        // In this request, we use a SHIB-MAIL attribute which does NOT match an EPerson.
        getClient().perform(get("/api/authn/shibboleth")
                .requestAttr("SHIB-MAIL", "not-an-eperson@example.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRedirectRequiresShibAttributes() throws Exception {
        // Verify this endpoint doesn't work if no SHIB-* attributes are set
        getClient().perform(get("/api/authn/shibboleth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRedirectRequiresShibAttributes2() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Verify this endpoint also doesn't work using a regular auth token (again if SHIB-* attributes missing)
        getClient(token).perform(get("/api/authn/shibboleth"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void patchPassword() throws Exception {

        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context)
                                        .withNameInMetadata("John", "Doe")
                                        .withEmail("Johndoe@example.com")
                                        .withPassword(password)
                                        .build();

        context.restoreAuthSystemState();

        String newPassword = "newpassword";

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/password", newPassword);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        // login through shibboleth
        String token = getClient().perform(get("/api/authn/shibboleth").requestAttr("SHIB-MAIL", eperson.getEmail()))
                                  .andExpect(status().is3xxRedirection())
                                  .andExpect(redirectedUrl("http://localhost:4000"))
                                  .andReturn().getResponse().getHeader("Authorization");


        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

        // updates password
        getClient(token).perform(patch("/api/eperson/epersons/" + ePerson.getID())
                                         .content(patchBody)
                                         .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                        .andExpect(status().isForbidden());

    }

}
