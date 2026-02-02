/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.random.RandomGenerator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.FastHttpDateFormat;
import org.dspace.services.ConfigurationService;
import org.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ALTCHA (Proof of Work, cookie-less) controller to handle challenge requests.
 * A salt, challenge hash will be sent and the client will have to calculate the number and send it back
 * to implementing controllers (e.g. request-a-copy) for validation.
 * The proof-of-work makes spam uneconomic, without requiring annoying puzzle tests or 3rd party services.
 * @see org.dspace.app.rest.repository.RequestItemRepository
 * @see <a href="https://altcha.org/docs/server-integration">Altcha docs></a>
 *
 * @author Kim Shepherd
 */
@RequestMapping(value = "/api/captcha")
@RestController
public class AltchaCaptchaRestController implements InitializingBean {
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    // Logger
    private static Logger log = LogManager.getLogger();

    // Default response expiry
    private static final long DEFAULT_EXPIRE_TIME = 60L * 60L * 1000L;

    /**
     * Calculate a challenge for ALTCHA captcha
     *
     * See https://altcha.org/docs/server-integration for implementation details and examples
     * @param request HTTP request
     * @param response HTTP response
     * @return response entity with JSON challenge for client to begin proof-of-work
     */
    @GetMapping("/challenge")
    @PreAuthorize("permitAll()")
    public ResponseEntity getAltchaChallenge(HttpServletRequest request, HttpServletResponse response) {

        // Set algorithm and hmac key
        // Algorithm
        String algorithm = configurationService.getProperty("altcha.algorithm", "SHA-256");
        String hmacKey = configurationService.getProperty("altcha.hmac.key");
        if (hmacKey == null) {
            log.error("hmac key not found, see: altcha.hmac.key in altcha.cfg");
            return ControllerUtils.toEmptyResponse(HttpStatus.BAD_REQUEST);
        }

        // Instantiate random generator
        RandomGenerator generator = RandomGenerator.getDefault();

        // Generate a random salt
        String randomSalt = bytesToHex(generateSalt());

        // Generate a random integer and write as string, 0 - 100000
        // This number is kept fairly low to keep proof of work at a decent trade-off for the client
        // We want to dissuade spammers, while keeping form functionality smooth for the user
        int randomNumber = generator.nextInt(100000);
        String randomNumberString = String.valueOf(randomNumber);

        // Generate the challenge as a hex string sha256 hash of concatenated salt and random string
        try {
            // Challenge = sha256 of salt + secret
            String challenge = calculateHash(randomSalt + randomNumberString, algorithm);
            if (StringUtils.isBlank(challenge)) {
                log.error("Error generating altcha challenge");
                // Default, return no content
                return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            // HMAC signature, using configured HMAC key and the generated challenge string
            String hmac = new HmacUtils("HmacSHA256", hmacKey).hmacHex(challenge);
            if (StringUtils.isBlank(hmac)) {
                log.error("Error generating HMAC signature");
                // Default, return no content
                return ControllerUtils.toEmptyResponse(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Set response body and headers
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("algorithm", algorithm);
            jsonObject.put("challenge", challenge);
            jsonObject.put("salt", randomSalt);
            jsonObject.put("signature", hmac);
            String body = jsonObject.toString();
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.put("Cache-Control", Collections.singletonList("private,no-cache"));
            httpHeaders.put("Expires", Collections.singletonList(FastHttpDateFormat.formatDate(
                    System.currentTimeMillis() + DEFAULT_EXPIRE_TIME)));
            httpHeaders.put("Content-Type", Collections.singletonList("application/json"));
            httpHeaders.put("Content-Length", Collections.singletonList(
                    String.valueOf(body.getBytes(StandardCharsets.UTF_8).length)));
            return ResponseEntity.ok().headers(httpHeaders).body(body);

        } catch (NoSuchAlgorithmException e) {
            log.error("No such algorithm: {}, {}", algorithm, e.getMessage());
            return ControllerUtils.toEmptyResponse(HttpStatus.BAD_REQUEST);
        }


    }

    /**
     * Makes a salt like 0c9c5ef19f
     * Kept fairly simple as all we want is basic proof-of-work from the client
     * @return salt string
     */
    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[5];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Encode bytes to hex string
     * @param bytes bytes to encode
     * @return hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

    /**
     * Calculate a hex string from a digest, given an input string
     * @param input input string
     * @param algorithm algorithm key, eg. SHA-256
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String calculateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = sha256.digest(input.getBytes());
        return bytesToHex(hashBytes);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        discoverableEndpointsService
                .register(this, Arrays
                        .asList(Link.of("/api/captcha", "captcha")));
    }
}
