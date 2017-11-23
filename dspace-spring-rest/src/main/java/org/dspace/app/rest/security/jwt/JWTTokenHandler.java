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
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;

/**
 * Class responsible for creating and parsing JWTs, supports both JWS and JWE
 *
 * @author Atmire NV (info at atmire dot com)
 */
@Component
public class JWTTokenHandler implements InitializingBean {

    private static final int MAX_CLOCK_SKEW_SECONDS = 60;
    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    @Autowired
    private List<JWTClaimProvider> jwtClaimProviders;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonClaimProvider ePersonClaimProvider;

    @Autowired
    private EPersonService ePersonService;

    private String jwtKey;
    private long expirationTime;
    private boolean includeIP;
    private boolean encryptionEnabled;
    private boolean compressionEnabled;
    private byte[] encryptionKey;


    @Override
    public void afterPropertiesSet() throws Exception {
        this.jwtKey = getSecret("jwt.token.secret");
        this.encryptionKey = getSecret("jwt.encryption.secret").getBytes();

        this.expirationTime = configurationService.getLongProperty("jwt.token.expiration", 30) * 60 * 1000;
        this.includeIP = configurationService.getBooleanProperty("jwt.token.include.ip", true);
        this.encryptionEnabled = configurationService.getBooleanProperty("jwt.encryption.enabled", false);
        this.compressionEnabled = configurationService.getBooleanProperty("jwt.compression.enabled", false);
    }

    /**
     * Retrieve EPerson from a jwt
     *
     * @param token
     * @param request
     * @param context
     * @return
     * @throws JOSEException
     * @throws ParseException
     * @throws SQLException
     */
    public EPerson parseEPersonFromToken(String token, HttpServletRequest request, Context context) throws JOSEException, ParseException, SQLException {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        SignedJWT signedJWT = getSignedJWT(token);

        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

        EPerson ePerson = getEPerson(context, jwtClaimsSet);

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
     * Create a jwt with the EPerson details in it
     *
     * @param context
     * @param request
     * @param previousLoginDate
     * @param groups
     * @return
     * @throws JOSEException
     */
    public String createTokenForEPerson(Context context, HttpServletRequest request, Date previousLoginDate, List<Group> groups) throws JOSEException, SQLException {

        EPerson ePerson = updateSessionSalt(context, previousLoginDate);

        JWTClaimsSet claimsSet = buildJwtClaimsSet(context, request);

        SignedJWT signedJWT = createSignedJWT(request, ePerson, claimsSet);

        String token;
        if (isEncryptionEnabled()) {
            token = encryptJWT(signedJWT).serialize();
        } else {
            token = signedJWT.serialize();
        }

        return token;
    }

    public void invalidateToken(String token, HttpServletRequest request, Context context) throws Exception {
        if (StringUtils.isNotBlank(token)) {

            EPerson ePerson = parseEPersonFromToken(token, request, context);
            if (ePerson != null) {
                ePerson.setSessionSalt("");
            }

        }
    }

    public long getExpirationPeriod() {
        return expirationTime;
    }


    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public byte[] getEncryptionKey() {
        return encryptionKey;
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

    private boolean isValidToken(HttpServletRequest request, SignedJWT signedJWT, JWTClaimsSet jwtClaimsSet, EPerson ePerson) throws JOSEException {
        if(StringUtils.isBlank(ePerson.getSessionSalt())) {
            return false;
        } else {
            JWSVerifier verifier = new MACVerifier(buildSigningKey(request, ePerson));

            //If token is valid and not expired return eperson in token
            Date expirationTime = jwtClaimsSet.getExpirationTime();
            return signedJWT.verify(verifier)
                    && expirationTime != null
                    //Ensure expiration timestamp is after the current time, with a minute of acceptable clock skew.
                    && DateUtils.isAfter(expirationTime, new Date(), MAX_CLOCK_SKEW_SECONDS);
        }
    }

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

    private EPerson getEPerson(Context context, JWTClaimsSet jwtClaimsSet) throws SQLException {
        return ePersonClaimProvider.getEPerson(context, jwtClaimsSet);
    }

    private SignedJWT createSignedJWT(HttpServletRequest request, EPerson ePerson, JWTClaimsSet claimsSet) throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        JWSSigner signer = new MACSigner(buildSigningKey(request, ePerson));
        signedJWT.sign(signer);
        return signedJWT;
    }

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
        if (compressionEnabled) {
            return builder.compressionAlgorithm(CompressionAlgorithm.DEF);
        }
        return builder;
    }

    /**
     * This returns the key used for signing the token. This key is at least 256 bits/32 bytes (server key has minimum length of 1 byte and the eperson session salt is always 32 bytes),
     * this way the key is always long enough for the HMAC using SHA-256 algorithm.
     * More information: https://tools.ietf.org/html/rfc7518#section-3.2
     *
     * @param request
     * @param ePerson
     * @return
     */
    private String buildSigningKey(HttpServletRequest request, EPerson ePerson) {
        String ipAddress = "";
        if (includeIP) {
            ipAddress = getIpAddress(request);
        }
        return jwtKey + ePerson.getSessionSalt() + ipAddress;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private EPerson updateSessionSalt(final Context context, final Date previousLoginDate) throws SQLException {
        EPerson ePerson;

        try {
            ePerson = context.getCurrentUser();

            //If the previous login was within the configured token expiration time, we reuse the session salt.
            //This allows a user to login on multiple devices/browsers at the same time.
            if (StringUtils.isBlank(ePerson.getSessionSalt())
                    || previousLoginDate == null
                    || (ePerson.getLastActive().getTime() - previousLoginDate.getTime() > expirationTime)) {

                ePerson.setSessionSalt(generateRandomKey());
                ePersonService.update(context, ePerson);
            }

        } catch (AuthorizeException e) {
            ePerson = null;
        }

        return ePerson;
    }

    private String getSecret(String property) {
        String secret = configurationService.getProperty(property);

        if (StringUtils.isBlank(secret)) {
            secret = generateRandomKey();
        }

        return secret;
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
