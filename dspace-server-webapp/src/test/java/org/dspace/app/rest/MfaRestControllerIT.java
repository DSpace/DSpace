/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.mfa.service.MfaService;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for MFA REST endpoints.
 */
public class MfaRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private ObjectMapper objectMapper;

    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);
    private final SystemTimeProvider timeProvider = new SystemTimeProvider();

    @Before
    public void setup() {
        configurationService.setProperty("mfa.totp.enabled", true);
        configurationService.setProperty("mfa.totp.mandatory", false);
    }

    @Test
    public void testMfaStatusWhenNotEnrolled() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/mfa/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)))
            .andExpect(jsonPath("$.globallyEnabled", is(true)))
            .andExpect(jsonPath("$.mandatory", is(false)));
    }

    @Test
    public void testMfaStatusAnonymousReturns401() throws Exception {
        getClient().perform(get("/api/authn/mfa/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMfaSetupReturnsSecretAndUri() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/authn/mfa/setup")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.secret", notNullValue()))
            .andExpect(jsonPath("$.provisioningUri", notNullValue()));
    }

    @Test
    public void testMfaSetupWhenDisabledGloballyReturns403() throws Exception {
        configurationService.setProperty("mfa.totp.enabled", false);

        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(post("/api/authn/mfa/setup")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testFullMfaEnrollmentFlow() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Setup - get secret
        String setupResponse = getClient(token).perform(post("/api/authn/mfa/setup")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        Map<String, String> setup = objectMapper.readValue(setupResponse, Map.class);
        String secret = setup.get("secret");

        // Generate a valid TOTP code from the secret
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String validCode = codeGenerator.generate(secret, counter);

        // Verify setup with the code
        getClient(token).perform(post("/api/authn/mfa/verify-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", validCode))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recoveryCodes", notNullValue()));

        // Verify MFA is now enabled
        getClient(token).perform(get("/api/authn/mfa/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    public void testVerifySetupWithInvalidCodeReturns401() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        // Setup first
        getClient(token).perform(post("/api/authn/mfa/setup")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        // Try to verify with invalid code
        getClient(token).perform(post("/api/authn/mfa/verify-setup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "000000"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginWithMfaEnabledReturnsMfaPendingToken() throws Exception {
        // First enable MFA for eperson
        context.turnOffAuthorisationSystem();
        var mfa = mfaService.initSetup(context, eperson);
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String code = codeGenerator.generate(mfa.getSecret(), counter);
        mfaService.verifyAndEnable(context, eperson, code);
        mfaService.generateRecoveryCodes(context, eperson);
        context.restoreAuthSystemState();
        context.commit();

        // Login - should get a token with mfa_verified=false
        String pendingToken = getAuthToken(eperson.getEmail(), password);

        // The token should be MFA-pending (mfa_verified=false in claims)
        // Accessing a regular endpoint should be blocked
        getClient(pendingToken).perform(get("/api/core/communities"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mfa_required", is(true)));
    }

    @Test
    public void testMfaVerifyWithValidCode() throws Exception {
        // Enable MFA for eperson
        context.turnOffAuthorisationSystem();
        var mfa = mfaService.initSetup(context, eperson);
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String setupCode = codeGenerator.generate(mfa.getSecret(), counter);
        mfaService.verifyAndEnable(context, eperson, setupCode);
        mfaService.generateRecoveryCodes(context, eperson);
        context.restoreAuthSystemState();
        context.commit();

        // Login
        String pendingToken = getAuthToken(eperson.getEmail(), password);

        // Generate a fresh TOTP code for verification
        counter = Math.floorDiv(timeProvider.getTime(), 30);
        String verifyCode = codeGenerator.generate(mfa.getSecret(), counter);

        // Verify MFA
        getClient(pendingToken).perform(post("/api/authn/mfa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", verifyCode))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("verified")));
    }

    @Test
    public void testMfaVerifyWithInvalidCodeReturns401() throws Exception {
        // Enable MFA for eperson
        context.turnOffAuthorisationSystem();
        var mfa = mfaService.initSetup(context, eperson);
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String setupCode = codeGenerator.generate(mfa.getSecret(), counter);
        mfaService.verifyAndEnable(context, eperson, setupCode);
        context.restoreAuthSystemState();
        context.commit();

        String pendingToken = getAuthToken(eperson.getEmail(), password);

        // Try with bad code
        getClient(pendingToken).perform(post("/api/authn/mfa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", "999999"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMfaDisableWithValidCode() throws Exception {
        // Enable MFA for eperson
        context.turnOffAuthorisationSystem();
        var mfa = mfaService.initSetup(context, eperson);
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String setupCode = codeGenerator.generate(mfa.getSecret(), counter);
        mfaService.verifyAndEnable(context, eperson, setupCode);
        context.restoreAuthSystemState();
        context.commit();

        // Get a fully verified token (simulate MFA complete)
        String token = getAuthToken(eperson.getEmail(), password);

        // Generate current TOTP for disable operation
        counter = Math.floorDiv(timeProvider.getTime(), 30);
        String disableCode = codeGenerator.generate(mfa.getSecret(), counter);

        // Disable MFA (note: this will work because /mfa/verify and /mfa/disable are whitelisted)
        getClient(token).perform(post("/api/authn/mfa/disable")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", disableCode))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("disabled")));

        // Verify MFA is now disabled
        String newToken = getAuthToken(eperson.getEmail(), password);
        getClient(newToken).perform(get("/api/authn/mfa/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    public void testAdminCanDisableMfaForUser() throws Exception {
        // Enable MFA for eperson
        context.turnOffAuthorisationSystem();
        var mfa = mfaService.initSetup(context, eperson);
        long counter = Math.floorDiv(timeProvider.getTime(), 30);
        String setupCode = codeGenerator.generate(mfa.getSecret(), counter);
        mfaService.verifyAndEnable(context, eperson, setupCode);
        context.restoreAuthSystemState();
        context.commit();

        // Admin disables MFA for eperson
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/authn/mfa/admin/" + eperson.getID() + "/disable")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("disabled")));
    }

    @Test
    public void testNonAdminCannotUseAdminEndpoints() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);

        getClient(token).perform(get("/api/authn/mfa/admin/" + admin.getID() + "/status"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testMandatoryMfaBlocksUserWithoutEnrollment() throws Exception {
        configurationService.setProperty("mfa.totp.mandatory", true);

        // Login as user without MFA enrolled
        String token = getAuthToken(eperson.getEmail(), password);

        // Should be blocked from regular endpoints
        getClient(token).perform(get("/api/core/communities"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mfa_required", is(true)));

        // But status endpoint should work
        getClient(token).perform(get("/api/authn/mfa/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.setupRequired", is(true)));
    }
}
