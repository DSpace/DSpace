/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import com.nimbusds.jose.*;
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

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Component
public class JWTTokenHandler implements InitializingBean {


    @Autowired
    private List<JWTClaimProvider> jwtClaimProviders;

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    private String jwtKey;
    private long expirationTime;
    private boolean includeIP;
    private boolean encryptionEnabled;
    private boolean compressionEnabled;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonClaimProvider ePersonClaimProvider;

    @Autowired
    private EPersonService ePersonService;

    private byte[] encryptionKey;


    @Override
    public void afterPropertiesSet() throws Exception {
        this.jwtKey = configurationService.getProperty("jwt.token.secret", "defaultjwtkeysecret");
        this.expirationTime = configurationService.getLongProperty("jwt.token.expiration", 30) * 60 * 1000;
        this.includeIP = configurationService.getBooleanProperty("jwt.token.include.ip", true);
        this.encryptionEnabled = configurationService.getBooleanProperty("jwt.encryption.enabled", false);
        this.compressionEnabled = configurationService.getBooleanProperty("jwt.compression.enabled", false);
        //TODO Don't reuse this all the time
        BytesKeyGenerator keyGen = KeyGenerators.secureRandom(16);
        encryptionKey = keyGen.generateKey();
    }

    /**
     * Retrieve EPerson from a jwt
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
        SignedJWT signedJWT;

        if (encryptionEnabled) {
            JWEObject jweObject = JWEObject.parse(token);
            jweObject.decrypt(new DirectDecrypter(encryptionKey));
            signedJWT = jweObject.getPayload().toSignedJWT();
        } else {
            signedJWT = SignedJWT.parse(token);
        }

        JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();

        EPerson ePerson = getEPerson(context, jwtClaimsSet);

        JWSVerifier verifier = new MACVerifier(buildSigningKey(request, ePerson));

        //If token is valid and not expired return eperson in token
        Date expirationTime = jwtClaimsSet.getExpirationTime();
        if (signedJWT.verify(verifier)
                && expirationTime != null
                //Ensure expiration timestamp is after the current time, with a minute of acceptable clock skew.
                && DateUtils.isAfter(expirationTime, new Date(), 60)) {

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

    private EPerson getEPerson(Context context, JWTClaimsSet jwtClaimsSet) throws SQLException {
        return ePersonClaimProvider.getEPerson(context, jwtClaimsSet);
    }

    /**
     * Create a jwt with the EPerson details in it
     * @param context
     * @param request
     * @param previousLoginDate
     * @param groups
     * @return
     * @throws JOSEException
     */
    public String createTokenForEPerson(Context context, HttpServletRequest request, Date previousLoginDate, List<Group> groups) throws JOSEException {

        EPerson ePerson = updateSessionSalt(context, previousLoginDate);

        JWSSigner signer = new MACSigner(buildSigningKey(request, ePerson));
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

        for (JWTClaimProvider jwtClaimProvider : jwtClaimProviders) {
            builder = builder.claim(jwtClaimProvider.getKey(), jwtClaimProvider.getValue(context, request));
        }

        JWTClaimsSet claimsSet = builder
                .expirationTime(new Date(System.currentTimeMillis() + getExpirationTime()))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        signedJWT.sign(signer);

        if (encryptionEnabled) {
            JWEObject jweObject = new JWEObject(
                    compression(new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A128GCM)
                            .contentType("JWT"))

                            .build(), new Payload(signedJWT)
            );

            jweObject.encrypt(new DirectEncrypter(encryptionKey));


            return jweObject.serialize();
        } else {
            return signedJWT.serialize();
        }
    }

    //This method makes compression configurable
    private JWEHeader.Builder compression(JWEHeader.Builder builder) {
        if (compressionEnabled) {
            return builder.compressionAlgorithm(CompressionAlgorithm.DEF);
        }
        return builder;
    }


    public void invalidateToken(String token, HttpServletRequest request, Context context) {
        if (StringUtils.isNotBlank(token)) {
            try {
                EPerson ePerson = parseEPersonFromToken(token, request, context);
                if (ePerson != null) {
                    ePerson.setSessionSalt("");
                }
            } catch (Exception e) {
                log.warn("Error while parsing the token:", e);
            }
        }
    }

    /**
     * This returns the key used for signing the token. This key is at least 256 bits/32 bytes (server key has minimum length of 1 byte and the eperson session salt is always 32 bytes),
     * this way the key is always long enough for the HMAC using SHA-256 algorithm.
     * More information: https://tools.ietf.org/html/rfc7518#section-3.2
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


    private EPerson updateSessionSalt(final Context context, final Date previousLoginDate) {
        EPerson ePerson = null;

        try {
            ePerson = context.getCurrentUser();

            //If the previous login was within the configured token expiration time, we reuse the session salt.
            //This allows a user to login on multiple devices/browsers at the same time.
            if (previousLoginDate == null || (ePerson.getLastActive().getTime() - previousLoginDate.getTime() > expirationTime)) {
                ePerson.setSessionSalt(generateRandomSalt());
                ePersonService.update(context, ePerson);
            }

        } catch (SQLException e) {
            return null;
        } catch (AuthorizeException e) {
            return null;
        }

        return ePerson;
    }

    //Generate a random 32 byte salt
    private String generateRandomSalt() {
        //24 bytes because BASE64 encoding makes this 32 bytes
        BytesKeyGenerator bytesKeyGenerator = KeyGenerators.secureRandom(24);
        byte[] secretKey = bytesKeyGenerator.generateKey();
        return Base64.encodeBase64String(secretKey);
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
