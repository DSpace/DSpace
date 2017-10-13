package org.dspace.app.rest.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
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

    private static String jwtKey = "testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest";
    private AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    private EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    public static final String EPERSON_ID = "eid";
    public static final String SPECIAL_GROUPS = "sg";

    public EPerson parseEPersonFromToken(String token, HttpServletRequest request) throws JOSEException, ParseException, SQLException {

        SignedJWT signedJWT = SignedJWT.parse(token);
        Context context = new Context();
        EPerson ePerson = ePersonService.find(context, UUID.fromString(signedJWT.getJWTClaimsSet().getClaim(EPERSON_ID).toString()));
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        JWSVerifier verifier = new MACVerifier(jwtKey + ePerson.getSessionSalt() + ipAddress);

        //If token is valid and not expired return eperson in token
        if (signedJWT.verify(verifier) && new Date().getTime() < signedJWT.getJWTClaimsSet().getExpirationTime().getTime()) {
            return ePerson;
        } else {
            return null;
        }
    }


    public String createTokenForEPerson(Context context, HttpServletRequest request, EPerson ePerson, List<Group> groups) throws JOSEException {
        StringKeyGenerator stringKeyGenerator = KeyGenerators.string();
        String salt = stringKeyGenerator.generateKey();
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        JWSSigner signer = new MACSigner(jwtKey + salt + ipAddress);

        List<String> groupIds = groups.stream().map(group -> group.getID().toString()).collect(Collectors.toList());
        ePerson.setSessionSalt(salt);
        try {
            context.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(EPERSON_ID, ePerson.getID().toString())
                .claim(SPECIAL_GROUPS, groupIds)
                //TODO Expiration time configurable in config
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

}
