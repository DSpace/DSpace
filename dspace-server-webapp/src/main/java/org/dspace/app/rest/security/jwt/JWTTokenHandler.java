/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.CompressionAlgorithm;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.service.ClientInfoService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 * Class responsible for creating and parsing JSON Web Tokens (JWTs), supports both JWS and JWE
 * https://jwt.io/ . This abstract class needs to be extended with a class providing the
 * configuration keys for the particular type of token.
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public abstract class JWTTokenHandler {

    private static final int MAX_CLOCK_SKEW_SECONDS = 60;
    private static final String AUTHORIZATION_TOKEN_PARAMETER = "authentication-token";

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    @Autowired
    private List<JWTClaimProvider> jwtClaimProviders;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonClaimProvider ePersonClaimProvider;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ClientInfoService clientInfoService;

    private String generatedJwtKey;
    private String generatedEncryptionKey;

    /**
     * Get the configuration property key for the token secret.
     * @return the configuration property key
     */
    protected abstract String getTokenSecretConfigurationKey();

    /**
     * Get the configuration property key for the encryption secret.
     * @return the configuration property key
     */
    protected abstract String getEncryptionSecretConfigurationKey();

    /**
     * Get the configuration property key for the expiration time.
     * @return the configuration property key
     */
    protected abstract String getTokenExpirationConfigurationKey();

    /**
     * Get the configuration property key for the encryption enable setting.
     * @return the configuration property key
     */
    protected abstract String getEncryptionEnabledConfigurationKey();

    /**
     * Get the configuration property key for the compression enable setting.
     * @return the configuration property key
     */
    protected abstract String getCompressionEnabledConfigurationKey();

    /**
     * Retrieve EPerson from a JSON Web Token (JWT)
     *
     * @param token token as a string
     * @param request current request
     * @param context current Context
     * @return DSpace EPerson object parsed from the token
     * @throws JOSEException
     * @throws ParseException
     * @throws SQLException
     */
    public EPerson parseEPersonFromToken(String token, HttpServletRequest request, Context context)
        throws JOSEException, ParseException, SQLException {
        if (StringUtils.isBlank(token)) {
            return null;
        }
        // parse/decrypt the token
        SignedJWT signedJWT = getSignedJWT(token);
        // get the claims set from the parsed token
        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
        // retrieve the EPerson from the claims set
        EPerson ePerson = getEPerson(context, jwtClaimsSet);

        // As long as the JWT is valid, parse all claims and return the EPerson
        if (isValidToken(request, signedJWT, jwtClaimsSet, ePerson)) {

            log.debug("Received valid token for username: " + ePerson.getEmail());

            for (JWTClaimProvider jwtClaimProvider : jwtClaimProviders) {
                jwtClaimProvider.parseClaim(context, request, jwtClaimsSet);
            }

            return ePerson;
        } else {
            log.warn(getIpAddress(request) + " tried to use an expired or non-valid token");
            return null;
        }
    }

    /**
     * Create a JWT with the EPerson details in it
     *
     * @param context current Context
     * @param request current Request
     * @param previousLoginDate date of last login (before this one)
     * @return string version of signed JWT
     * @throws JOSEException
     */
    public String createTokenForEPerson(Context context, HttpServletRequest request, Date previousLoginDate)
        throws JOSEException, SQLException {

        // Verify that the user isn't trying to use a short lived token to generate another token
        if (StringUtils.isNotBlank(request.getParameter(AUTHORIZATION_TOKEN_PARAMETER))) {
            throw new AccessDeniedException("Short lived tokens can't be used to generate other tokens");
        }

        // Update the saved session salt for the currently logged in user, returning the user object
        EPerson ePerson = updateSessionSalt(context, previousLoginDate);

        // Create a claims set based on currently logged in user
        JWTClaimsSet claimsSet = buildJwtClaimsSet(context, request);

        // Create a signed JWT from those two things
        SignedJWT signedJWT = createSignedJWT(request, ePerson, claimsSet);

        String token;
        if (isEncryptionEnabled()) {
            token = encryptJWT(signedJWT).serialize();
        } else {
            token = signedJWT.serialize();
        }

        return token;
    }

    /**
     * Invalidate the current Java Web Token (JWT) in the current request
     * @param token current token
     * @param request current request
     * @param context current Context
     * @throws Exception
     */
    public void invalidateToken(String token, HttpServletRequest request, Context context) throws Exception {
        if (StringUtils.isNotBlank(token)) {

            EPerson ePerson = parseEPersonFromToken(token, request, context);
            if (ePerson != null) {
                ePerson.setSessionSalt("");
            }

        }
    }

    /**
     * Retrieve the token secret key from configuration. If not specified, generate and cache a random 32 byte key
     * @return configuration value or random 32 byte key
     */
    public String getJwtKey() {
        String secret = configurationService.getProperty(getTokenSecretConfigurationKey());

        if (StringUtils.isBlank(secret)) {
            if (StringUtils.isBlank(generatedJwtKey)) {
                generatedJwtKey = generateRandomKey();
            }
            secret = generatedJwtKey;
        }

        return secret;
    }

    public long getExpirationPeriod() {
        return configurationService.getLongProperty(getTokenExpirationConfigurationKey(), 1800000);
    }

    public boolean isEncryptionEnabled() {
        return configurationService.getBooleanProperty(getEncryptionEnabledConfigurationKey(), false);
    }

    public boolean getCompressionEnabled() {
        return configurationService.getBooleanProperty(getCompressionEnabledConfigurationKey(), false);
    }

    /**
     * Retrieve the encryption secret key from configuration. If not specified, generate and cache a random 32 byte key
     * @return configuration value or random 32 byte key
     */
    public byte[] getEncryptionKey() {
        String secretString = configurationService.getProperty(getEncryptionSecretConfigurationKey());

        if (StringUtils.isBlank(secretString)) {
            if (StringUtils.isBlank(generatedEncryptionKey)) {
                generatedEncryptionKey = generateRandomKey();
            }
            secretString = generatedEncryptionKey;
        }

        return secretString.getBytes();
    }

    private JWEObject encryptJWT(SignedJWT signedJWT) throws JOSEException {
        JWEObject jweObject = new JWEObject(
            compression(new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128GCM)
                            .contentType("JWT"))

                .build(), new Payload(signedJWT)
        );

        jweObject.encrypt(new DirectEncrypter(getEncryptionKey()));
        return jweObject;
    }

    /**
     * Determine if current JWT is valid for the given EPerson object.
     * To be valid, current JWT *must* have been signed by the EPerson and not be expired.
     * If EPerson is null or does not have a known active session, false is returned immediately.
     * @param request current request
     * @param signedJWT current signed JWT
     * @param jwtClaimsSet claims set of current JWT
     * @param ePerson EPerson parsed from current signed JWT
     * @return true if valid, false otherwise
     * @throws JOSEException
     */
    protected boolean isValidToken(HttpServletRequest request, SignedJWT signedJWT, JWTClaimsSet jwtClaimsSet,
                                 EPerson ePerson) throws JOSEException {
        if (ePerson == null || StringUtils.isBlank(ePerson.getSessionSalt())) {
            return false;
        } else {
            JWSVerifier verifier = new MACVerifier(buildSigningKey(ePerson));

            //If token is valid and not expired return eperson in token
            Date expirationTime = jwtClaimsSet.getExpirationTime();
            return signedJWT.verify(verifier)
                && expirationTime != null
                //Ensure expiration timestamp is after the current time, with a minute of acceptable clock skew.
                && DateUtils.isAfter(expirationTime, new Date(), MAX_CLOCK_SKEW_SECONDS);
        }
    }

    /**
     * Return the signed JWT.
     * If JWT encryption is enabled, decrypt the token and return.
     * Otherwise, parse the string into a signed JWT
     * @param token string token
     * @return parsed (possibly decrypted) SignedJWT
     * @throws ParseException
     * @throws JOSEException
     */
    private SignedJWT getSignedJWT(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT;

        if (isEncryptionEnabled()) {
            JWEObject jweObject = JWEObject.parse(token);
            jweObject.decrypt(new DirectDecrypter(getEncryptionKey()));
            signedJWT = jweObject.getPayload().toSignedJWT();
        } else {
            signedJWT = SignedJWT.parse(token);
        }

        return signedJWT;
    }

    /**
     * Based on the given JWT claims set (which should include an EPerson ID), locate the
     * corresponding EPerson in the current Context
     * @param context current context
     * @param jwtClaimsSet JWT claims set
     * @return EPerson object (or null, if not found)
     * @throws SQLException
     */
    private EPerson getEPerson(Context context, JWTClaimsSet jwtClaimsSet) throws SQLException {
        return ePersonClaimProvider.getEPerson(context, jwtClaimsSet);
    }

    /**
     * Create a signed JWT from the given EPerson and claims set.
     * @param request current request
     * @param ePerson EPerson to create signed JWT for
     * @param claimsSet claims set of JWT
     * @return signed JWT
     * @throws JOSEException
     */
    private SignedJWT createSignedJWT(HttpServletRequest request, EPerson ePerson, JWTClaimsSet claimsSet)
        throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(
            new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        JWSSigner signer = new MACSigner(buildSigningKey(ePerson));
        signedJWT.sign(signer);
        return signedJWT;
    }

    /**
     * Create a new JWT claims set based on the current Context (and currently logged in user).
     * Set its expiration time based on the configured expiration period.
     * @param context current Context
     * @param request current Request
     * @return new JWTClaimsSet
     */
    private JWTClaimsSet buildJwtClaimsSet(Context context, HttpServletRequest request) {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

        for (JWTClaimProvider jwtClaimProvider : jwtClaimProviders) {
            builder = builder.claim(jwtClaimProvider.getKey(), jwtClaimProvider.getValue(context, request));
        }

        return builder
            .expirationTime(new Date(System.currentTimeMillis() + getExpirationPeriod()))
            .build();
    }

    //This method makes compression configurable
    private JWEHeader.Builder compression(JWEHeader.Builder builder) {
        if (getCompressionEnabled()) {
            return builder.compressionAlgorithm(CompressionAlgorithm.DEF);
        }
        return builder;
    }

    /**
     * This returns the key used for signing the token. This key is at least 256 bits/32 bytes (server key has
     * minimum length of 1 byte and the eperson session salt is always 32 bytes),
     * this way the key is always long enough for the HMAC using SHA-256 algorithm.
     * More information: https://tools.ietf.org/html/rfc7518#section-3.2
     *
     * @param ePerson currently authenticated EPerson
     * @return signing key for token
     */
    protected String buildSigningKey(EPerson ePerson) {
        return getJwtKey() + ePerson.getSessionSalt();
    }

    /**
     * Get IP Address of client. Only used for logging purposes at this time
     * @param request current request
     * @return IP address of client
     */
    private String getIpAddress(HttpServletRequest request) {
        return clientInfoService.getClientIp(request);
    }


    /**
     * Update session salt information for the currently logged in user.
     * The session salt is a random key that is saved to EPerson object (and database table) and used to validate
     * a JWT on later requests.
     * @param context current DSpace Context
     * @param previousLoginDate date of last login (prior to this one)
     * @return EPerson object of current user, with an updated session salt
     * @throws SQLException
     */
    protected EPerson updateSessionSalt(final Context context, final Date previousLoginDate) throws SQLException {
        EPerson ePerson;

        try {
            ePerson = context.getCurrentUser();

            //If the previous login was within the configured token expiration time, we reuse the session salt.
            //This allows a user to login on multiple devices/browsers at the same time.
            if (StringUtils.isBlank(ePerson.getSessionSalt())
                || previousLoginDate == null
                || (ePerson.getLastActive().getTime() - previousLoginDate.getTime() > getExpirationPeriod())) {
                log.debug("Regenerating auth token as session salt was either empty or expired..");
                ePerson.setSessionSalt(generateRandomKey());
                ePersonService.update(context, ePerson);
            }

        } catch (AuthorizeException e) {
            ePerson = null;
        }

        return ePerson;
    }

    /**
     * Generate a random 32 bytes key
     */
    private String generateRandomKey() {
        //24 bytes because BASE64 encoding makes this 32 bytes
        //Base64 takes 4 characters for every 3 bytes

        BytesKeyGenerator bytesKeyGenerator = KeyGenerators.secureRandom(24);
        byte[] secretKey = bytesKeyGenerator.generateKey();
        return Base64.encodeBase64String(secretKey);
    }
}
