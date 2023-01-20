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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for testing the assigning the user to the groups based on the configuration
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinShibbolethAuthAssing2GroupsIT extends AbstractControllerIntegrationTest {

    public static final String[] SHIB_ONLY = {"org.dspace.authenticate.clarin.ClarinShibAuthentication"};

    private EPerson clarinEperson;

    @Autowired
    ConfigurationService configurationService;

    @Before
    public void setup() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        // Add a second trusted host for some tests
        configurationService.setProperty("rest.cors.allowed-origins",
                "${dspace.ui.url}, http://anotherdspacehost:4000");

        // Enable Shibboleth login for all tests
        configurationService.setProperty("plugin.sequence.org.dspace.authenticate.AuthenticationMethod", SHIB_ONLY);

        // Create an eperson with netID - that means the user already exist in the database
        clarinEperson = EPersonBuilder.createEPerson(context)
                .withCanLogin(false)
                .withEmail("clarin@email.com")
                .withNameInMetadata("first", "last")
                .withLanguage(I18nUtil.getDefaultLocale().getLanguage())
                .withNetId("123456789")
                .build();
        context.turnOffAuthorisationSystem();
    }


    // No specific SHIB group headers
    @Test
    public void shouldAssignToDefaultGroup() throws Exception {
        String groupName = configurationService.getProperty("authentication-shibboleth.default.auth.group");
        GroupBuilder.createGroup(context)
                .withName(groupName)
                .build();

        // The endpoint is accessible only for the user with the authority `AUTHENTICATED`
        getClient().perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isUnauthorized());

        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        // Check if is authenticated
        getClient(token).perform(get("/api/authn/status"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.authenticated", is(true)))
                        .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

        // Check if the user was assigned to the `Authenticated` group (it is a default group)
        getClient(token).perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isOk());
    }

    // Shib entitlement header = ufal.mff.cuni.cz
    // Group where will be assigned the user is loaded
    // from the `authentication-shibboleth.role.ufal.mff.cuni.cz` property
    @Test
    public void shouldAssignAuthorityBasedOnRoleCfgProperty() throws Exception {
        String groupName = "UFAL";
        GroupBuilder.createGroup(context)
                .withName(groupName)
                .build();

        String defaultGroupName = configurationService.getProperty("authentication-shibboleth.default.auth.group");
        GroupBuilder.createGroup(context)
                .withName(defaultGroupName)
                .build();

        // The endpoint is accessible only for the user with the authority `AUTHENTICATED`
        getClient().perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isUnauthorized());

        // The endpoint is accessible only for the user with the authority `UFAL`
        getClient().perform(get("/api/test/auth/ufal"))
                .andExpect(status().isUnauthorized());

        // The user will be added to the AUTHENTICATED and UFAL group
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid())
                        .header("entitlement", "ufal.mff.cuni.cz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        // Check if is authenticated
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

        // Check if the user was assigned to the `AUTHENTICATED` group (it is a default group)
        getClient(token).perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isOk());

        // Check if the user was assigned to the `UFAL` group (it is a default group)
        getClient(token).perform(get("/api/test/auth/ufal"))
                .andExpect(status().isOk());
    }

    // Shib entitlement header = ufal.mff.cuni.cz
    // Group where will be assigned the user is loaded
    // from the `authentication-shibboleth.header.entitlement` property
    @Test
    public void shouldAssingAuthorityBasedOfEntitlementCfgProperty() throws Exception {
        String groupName = "UFAL_MEMBER";
        GroupBuilder.createGroup(context)
                .withName(groupName)
                .build();

        String ufalGroupName = "UFAL";
        GroupBuilder.createGroup(context)
                .withName(ufalGroupName)
                .build();

        String defaultGroupName = configurationService.getProperty("authentication-shibboleth.default.auth.group");
        GroupBuilder.createGroup(context)
                .withName(defaultGroupName)
                .build();

        // The endpoint is accessible only for the user with the authority `AUTHENTICATED`
        getClient().perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isUnauthorized());

        // The endpoint is accessible only for the user with the authority `UFAL`
        getClient().perform(get("/api/test/auth/ufal-member"))
                .andExpect(status().isUnauthorized());

        // The user will be added to the AUTHENTICATED and UFAL group
        String token = getClient().perform(get("/api/authn/shibboleth")
                        .header("SHIB-MAIL", clarinEperson.getEmail())
                        .header("Shib-Identity-Provider", "Test idp")
                        .header("SHIB-NETID", clarinEperson.getNetid())
                        .header("entitlement", "staff@org1297.mff.cuni.cz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:4000"))
                .andReturn().getResponse().getHeader("Authorization");

        // Check if is authenticated
        getClient(token).perform(get("/api/authn/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.authenticationMethod", is("shibboleth")));

        // Check if the user was assigned to the `AUTHENTICATED` group (it is a default group)
        getClient(token).perform(get("/api/test/auth/authenticated"))
                .andExpect(status().isOk());

        // Check if the user was assigned to the `UFAL_MEMBER` group
        getClient(token).perform(get("/api/test/auth/ufal-member"))
                .andExpect(status().isOk());

        // Check if the user was NOT assigned to the `UFAL` group
        getClient(token).perform(get("/api/test/auth/ufal"))
                .andExpect(status().isForbidden());
    }
}
