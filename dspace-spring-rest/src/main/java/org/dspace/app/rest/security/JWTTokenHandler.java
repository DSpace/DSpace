/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JWTTokenHandler {

    private static final Logger log = LoggerFactory.getLogger(JWTTokenHandler.class);

    //TODO configurable through config files
    private String jwtKey;
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    public static final String EPERSON_ID = "eid";
    public static final String SPECIAL_GROUPS = "sg";


    public JWTTokenHandler() {
        jwtKey = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("jwt.secret", "defaultjwtkeysecret");
        System.out.println(jwtKey);
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

        SignedJWT signedJWT = SignedJWT.parse(token);
        EPerson ePerson = ePersonService.find(context, UUID.fromString(signedJWT.getJWTClaimsSet().getClaim(EPERSON_ID).toString()));
        String ipAddress = getIpAddress(request);
        JWSVerifier verifier = new MACVerifier(jwtKey + ePerson.getSessionSalt() + ipAddress);

        //If token is valid and not expired return eperson in token
        if (signedJWT.verify(verifier) && new Date().getTime() < signedJWT.getJWTClaimsSet().getExpirationTime().getTime()) {
            log.info("Received valid token for username: " + ePerson.getEmail());
            return ePerson;
        } else {
            log.info("Someone tried to use an expired or non-valid token");
            return null;
        }
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
        StringKeyGenerator stringKeyGenerator = KeyGenerators.string();
        String salt = stringKeyGenerator.generateKey();
        String ipAddress = getIpAddress(request);
        JWSSigner signer = new MACSigner(jwtKey + salt + ipAddress);

        List<String> groupIds = groups.stream().map(group -> group.getID().toString()).collect(Collectors.toList());
        ePerson.setSessionSalt(salt);
        try {
            context.commit();
        } catch (SQLException e) {
            log.error("Error while committing salt to eperson", e);
        }

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(EPERSON_ID, ePerson.getID().toString())
                .claim(SPECIAL_GROUPS, groupIds)
                //TODO Expiration time configurable in config
                .expirationTime(new Date(new Date().getTime() + 5 * 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }


    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

}
