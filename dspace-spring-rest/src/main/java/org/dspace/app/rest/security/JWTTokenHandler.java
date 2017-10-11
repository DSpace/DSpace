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

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JWTTokenHandler {

    private AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();


    public EPerson parseEPersonFromToken(String token) throws JOSEException, ParseException, SQLException {
        JWSVerifier verifier = new MACVerifier("testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest");
        SignedJWT signedJWT = SignedJWT.parse(token);
        if (signedJWT.verify(verifier)) {
            Context context = new Context();
            return EPersonServiceFactory.getInstance().getEPersonService().find(context, UUID.fromString(signedJWT.getJWTClaimsSet().getClaim("EPersonID").toString()));
        } else {
            return null;
        }
    }


    public String createTokenForEPerson(EPerson ePerson, List<Group> groups) throws JOSEException {
        JWSSigner signer = new MACSigner("testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest");

        List<Integer> groupIds = groups.stream().map(Group::getLegacyId).collect(Collectors.toList());

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
