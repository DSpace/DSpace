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
import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides a claim for a JSON Web Token, this claim is responsible for adding the authentication method to it
 */
@Component
public class AuthenticationMethodClaimProvider implements JWTClaimProvider {

    public static final String AUTHENTICATION_METHOD = "authenticationMethod";

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    public String getKey() {
        return AUTHENTICATION_METHOD;
    }

    @Override
    public Object getValue(final Context context, final HttpServletRequest request) {
        if (context.getAuthenticationMethod() != null) {
            return context.getAuthenticationMethod();
        }
        return authenticationService.getAuthenticationMethod(context, request);
    }

    @Override
    public void parseClaim(final Context context, final HttpServletRequest request, final JWTClaimsSet jwtClaimsSet)
            throws SQLException {
        try {
            context.setAuthenticationMethod(jwtClaimsSet.getStringClaim(AUTHENTICATION_METHOD));
        } catch (ParseException e) {
            log.error(e::getMessage, e);
        }
    }
}
