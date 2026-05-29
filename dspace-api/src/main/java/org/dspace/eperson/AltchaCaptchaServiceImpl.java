/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.ConfigurationService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic services implementation for a Proof of Work Captcha like Altcha.
 * Unlike Google ReCaptcha, there is no remote API or service to rely on, we simply
 * compare the client-side crypto puzzle challenge to our own work and pass or fail the validation that way
 * See https://altcha.org/docs/server-integration for implementation pseudocode.
 *
 * @see AltchaCaptchaController for REST impl
 *
 * @author Kim Shepherd
 */
public class AltchaCaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LogManager.getLogger(AltchaCaptchaServiceImpl.class);

    @Autowired
    private ConfigurationService configurationService;

    @PostConstruct
    public void init() {
    }

    /**
     * Process a response string and validate as per interface
     *
     * @param captchaPayloadHeader reCaptcha token to be validated
     * @param action action of reCaptcha
     * @throws InvalidReCaptchaException
     */
    @Override
    public void processResponse(String captchaPayloadHeader, String action) throws InvalidReCaptchaException {
        if (!validateAltchaCaptcha(captchaPayloadHeader)) {
            throw new InvalidReCaptchaException("ALTCHA captcha validation failed");
        }
    }

    /**
     * Validate captcha payload by reproducing the work
     * @param captchaPayloadHeader header conforming to altcha specs
     *                             See: https://altcha.org/docs/server-integration
     * @return
     */
    private boolean validateAltchaCaptcha(String captchaPayloadHeader) throws InvalidReCaptchaException {
        // Decode base64 string for parsing to json
        String captchaPayloadJson =
                new String(Base64.decodeBase64(captchaPayloadHeader.getBytes(StandardCharsets.UTF_8)));
        // Parse as JSON
        JSONObject captchaPayload = new JSONObject(captchaPayloadJson);
        // Extract data and validate work
        try {
            // Make sure the required fields are present
            if (captchaPayload.has("challenge") && captchaPayload.has("salt")
                    && captchaPayload.has("number") && captchaPayload.has("signature")
                    && captchaPayload.has("algorithm")) {
                String algorithm = captchaPayload.getString("algorithm");
                if (!"SHA-256".equals(algorithm)) {
                    throw new InvalidReCaptchaException("ALTCHA algorithm must be SHA-256, check config and payload");
                }
                // Get fields for code readability and debugging
                String challenge = captchaPayload.getString("challenge");
                String salt = captchaPayload.getString("salt");
                String number = String.valueOf(captchaPayload.getNumber("number"));
                String signature = captchaPayload.getString("signature");
                // Calculate hash
                String hash = CaptchaService.calculateHash(salt + number, captchaPayload.getString("algorithm"));
                // Get hmacKey
                String hmacKey = configurationService.getProperty("altcha.hmac.key");
                if (hmacKey == null) {
                    log.error("hmac key not found, see: altcha.hmac.key in altcha.cfg");
                    throw new InvalidReCaptchaException("hmac key not found");
                }
                // HMAC signature, using configured HMAC key and the generated challenge string
                String hmac = new HmacUtils("HmacSHA256", hmacKey).hmacHex(challenge);
                if (org.apache.commons.lang3.StringUtils.isBlank(hmac)) {
                    log.error("Error generating HMAC signature");
                    // Default, return no content
                    throw new InvalidReCaptchaException("error generating hmac signature");
                }
                // Compare received and expected values
                boolean challengeVerified = challenge.equals(hash);
                boolean signatureVerified = signature.equals(hmac);
                return challengeVerified && signatureVerified;
            }
        } catch (Exception e) {
            // If *any* error is enountered, throw InvalidReCaptchaException
            throw new InvalidReCaptchaException("Failed to validate ALTCHA captcha: " + e.getMessage());
        }

        // By default, fail the validation
        return false;
    }

}