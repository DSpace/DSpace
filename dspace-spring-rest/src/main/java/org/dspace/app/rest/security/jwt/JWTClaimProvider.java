/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;

/**
 * Interface to be implemented if you want to add a custom claim to a JSON Web Token, annotate with @Component
 * to include it's implementation in the token
 *
 * @author Atmire NV (info at atmire dot com)
 */
public interface JWTClaimProvider {

    String getKey();
    Object getValue(Context context, HttpServletRequest request);
    void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) throws SQLException;

}
