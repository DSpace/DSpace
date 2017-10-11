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

    public EPerson parseEPersonFromToken(String token) throws JOSEException, ParseException, SQLException {

        SignedJWT signedJWT = SignedJWT.parse(token);
        Context context = new Context();
        EPerson ePerson = ePersonService.find(context, UUID.fromString(signedJWT.getJWTClaimsSet().getClaim("EPersonID").toString()));
        JWSVerifier verifier = new MACVerifier(jwtKey + ePerson.getJwtSalt());
        if (signedJWT.verify(verifier)) {
            return ePerson;
        } else {
            return null;
        }
    }


    public String createTokenForEPerson(Context context, EPerson ePerson, List<Group> groups) throws JOSEException {
        StringKeyGenerator stringKeyGenerator = KeyGenerators.string();
        String salt = stringKeyGenerator.generateKey();
        JWSSigner signer = new MACSigner(jwtKey + salt);

        List<Integer> groupIds = groups.stream().map(Group::getLegacyId).collect(Collectors.toList());
        ePerson.setJwtSalt(salt);
        try {
            context.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("EPersonID", ePerson.getID().toString())
                .claim("special_groups", groupIds)
                .expirationTime(new Date(new Date().getTime() + 60 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256), claimsSet);

        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

}
