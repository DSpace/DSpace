/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.servicemanager.config.DSpaceConfigurationService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Component;

@Component
public class JWTTokenHandler {


    @Autowired
    private List<JWTClaimProvider> jwtClaimProviders;

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    private String jwtKey;
    private long expirationTime;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonClaimProvider ePersonClaimProvider;


    public JWTTokenHandler() {
        //TODO move properties to authentication module
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.jwtKey = configurationService.getProperty("jwt.token.secret", "defaultjwtkeysecret");
        this.expirationTime = configurationService.getLongProperty("jwt.token.expiration", 30) * 60 * 1000;
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
        if(StringUtils.isBlank(token)) {
            return null;
        }

        SignedJWT signedJWT = SignedJWT.parse(token);
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

            for (JWTClaimProvider jwtClaimProvider: jwtClaimProviders) {
                jwtClaimProvider.parseClaim(context, request, jwtClaimsSet);
            }

            return ePerson;
        } else {
            log.warn("Someone tried to use an expired or non-valid token");
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
     * @param ePerson
     * @param groups
     * @return
     * @throws JOSEException
     */
    public String createTokenForEPerson(Context context, HttpServletRequest request, EPerson ePerson, List<Group> groups) throws JOSEException {
        createNewSessionSalt(ePerson);
        context.setCurrentUser(ePerson);

        JWSSigner signer = new MACSigner(buildSigningKey(request, ePerson));


        try {
            context.commit();
        } catch (SQLException e) {
            //TODO FREDERIC throw exception and fail fast
            log.error("Error while committing salt to eperson", e);
        }

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();

        for (JWTClaimProvider jwtClaimProvider: jwtClaimProviders) {
            builder = builder.claim(jwtClaimProvider.getKey(), jwtClaimProvider.getValue(context, request));
        }

        JWTClaimsSet claimsSet = builder
                .expirationTime(new Date(System.currentTimeMillis() + expirationTime))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
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

    private String buildSigningKey(HttpServletRequest request, EPerson ePerson) {
        String ipAddress = getIpAddress(request);
        return jwtKey + ePerson.getSessionSalt() + ipAddress;
    }

    private String getIpAddress(HttpServletRequest request) {
        //TODO make using the ip address of the request optional
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private void createNewSessionSalt(EPerson ePerson) {
        StringKeyGenerator stringKeyGenerator = KeyGenerators.string();
        String salt = stringKeyGenerator.generateKey();
        ePerson.setSessionSalt(salt);
    }

    public List<JWTClaimProvider> getJwtClaimProviders() {
        return jwtClaimProviders;
    }

    public void setJwtClaimProviders(List<JWTClaimProvider> jwtClaimProviders) {
        this.jwtClaimProviders = jwtClaimProviders;
    }
}
