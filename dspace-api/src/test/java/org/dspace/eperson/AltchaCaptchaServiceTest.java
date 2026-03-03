/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.dspace.AbstractUnitTest;
import org.dspace.eperson.factory.CaptchaServiceFactory;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic tests to verity the Altcha captcha service can verify payloads from the client
 *
 * @author Kim Shepherd
 */
public class AltchaCaptchaServiceTest extends AbstractUnitTest {

    CaptchaService captchaService;
    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @After
    public void tearDown() {
        configurationService.setProperty("captcha.provider", "google");
    }
    @Before
    public void setUp() {
        configurationService.setProperty("captcha.provider", "altcha");
        configurationService.setProperty("altcha.hmac.key", "onetwothreesecret");
        captchaService = CaptchaServiceFactory.getInstance().getAltchaCaptchaService();
    }

    @Test
    public void testValidJSONCaptchaPayloadValidation() {
        // Create raw JSON first, using previous known-good payload with our test hmac secret of "onetwothreesecret"
        JSONObject json = new JSONObject();
        json.put("algorithm", "SHA-256");
        json.put("salt", "dcf5eba26e");
        json.put("number", 4791);
        json.put("challenge", "0d8dd34089fdd610bd9a8857ea1fa4a5f9fe4b53f5df0c4e1eff6dc987c4d2bf");
        json.put("signature", "dfe4ec56f3d61e3a021b1c3b3ea4c7d6aea9812ab719ffe130fd386ce0b4158c");
        // Base64 encode it
        String payload = Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));

        // Now validate
        captchaService.processResponse(payload, "validate");
    }

    @Test(expected = InvalidReCaptchaException.class)
    public void testInvalidCaptchaPayloadValidation() {
        // Create raw JSON first
        JSONObject json = new JSONObject();
        json.put("algorithm", "SHA-256");
        json.put("challenge", "abcdefg");
        json.put("salt", "salt123");
        json.put("number", 1);
        json.put("signature", "123123123123");
        String payload = Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
        // Ask the captcha service to validate the payload
        captchaService.processResponse(payload, "validate");
        // If we got here, something is off - an exception should have been thrown
        fail("Invalid captcha payload should have failed with IllegalReCaptchaException");
    }
}
