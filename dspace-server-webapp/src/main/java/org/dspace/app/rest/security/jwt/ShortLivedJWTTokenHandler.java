/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.util.Date;
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Component;

/**
 * Class responsible for creating and parsing JSON Web Tokens (JWTs) used for bitstream
 * downloads among other things, supports both JWS and JWE https://jwt.io/ .
 */
@Component
public class ShortLivedJWTTokenHandler extends JWTTokenHandler {

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
    @Override
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
                //Ensure expiration timestamp is after the current time
                && DateUtils.isAfter(expirationTime, new Date(), 0);
        }
    }

    /**
     * The session salt doesn't need to be updated for short lived tokens.
     * @param context current DSpace Context
     * @param previousLoginDate date of last login (prior to this one)
     * @return EPerson object of current user, with an updated session salt
     */
    @Override
    protected EPerson updateSessionSalt(final Context context, final Date previousLoginDate) {
        return context.getCurrentUser();
    }

    @Override
    protected String getTokenSecretConfigurationKey() {
        return "jwt.shortLived.token.secret";
    }

    @Override
    protected String getEncryptionSecretConfigurationKey() {
        return "jwt.shortLived.encryption.secret";
    }

    @Override
    protected String getTokenExpirationConfigurationKey() {
        return "jwt.shortLived.token.expiration";
    }

    @Override
    protected String getEncryptionEnabledConfigurationKey() {
        return "jwt.shortLived.encryption.enabled";
    }

    @Override
    protected String getCompressionEnabledConfigurationKey() {
        return "jwt.shortLived.compression.enabled";
    }
}
