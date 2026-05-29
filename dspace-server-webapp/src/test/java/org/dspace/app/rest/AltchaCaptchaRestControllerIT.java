/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test basic challenge method in AltchaCaptchaRestController
 * Actual payload validation tests are done in AltchaCaptchaServiceTest
 *
 * @author Kim Shepherd
 */
public class AltchaCaptchaRestControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Before
    public void setup() {
        configurationService.setProperty("captcha.provider", "altcha");
        configurationService.setProperty("altcha.hmac.key", "onetwothreesecret");
    }

    @After
    public void tearDown() {
        configurationService.setProperty("captcha.provider", "google");
    }

    @Test
    public void testGetAltchaChallengeAuthenticated() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);

        getClient(authToken).perform(get("/api/captcha/challenge"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(header().exists("Expires"))
                .andExpect(jsonPath("$.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.challenge").isString())
                .andExpect(jsonPath("$.salt").isString())
                .andExpect(jsonPath("$.signature").isString());
    }

    @Test
    public void testGetAltchaChallengeUnauthenticated() throws Exception {
        getClient().perform(get("/api/captcha/challenge"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.algorithm").value("SHA-256"))
                .andExpect(jsonPath("$.challenge").isString())
                .andExpect(jsonPath("$.salt").isString())
                .andExpect(jsonPath("$.signature").isString());
    }

    @Test
    public void testGetAltchaChallengeWithMissingHmacKey() throws Exception {
        // Temporarily clear the HMAC key config
        configurationService.setProperty("altcha.hmac.key", null);

        getClient().perform(get("/api/captcha/challenge"))
                .andExpect(status().isBadRequest());

        // Reset the config
        configurationService.setProperty("altcha.hmac.key", "onetwothreesecret");
    }

}
