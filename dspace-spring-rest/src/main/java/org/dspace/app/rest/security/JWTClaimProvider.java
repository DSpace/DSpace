package org.dspace.app.rest.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

public interface JWTClaimProvider {

    String getKey();
    Object getValue(Context context, HttpServletRequest request);
    void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) throws SQLException;

}
