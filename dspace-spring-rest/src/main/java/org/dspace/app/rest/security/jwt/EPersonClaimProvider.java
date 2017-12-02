/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Provides a claim for a JSON Web Token, this claim is responsible for adding the EPerson ID to it
 *
 * @author Atmire NV (info at atmire dot com)
 */
@Component
public class EPersonClaimProvider implements JWTClaimProvider{

    public static final String EPERSON_ID = "eid";

    @Autowired
    private EPersonService ePersonService;

    public String getKey() {
        return EPERSON_ID;
    }

    public Object getValue(Context context, HttpServletRequest request) {
        return context.getCurrentUser().getID().toString();
    }

    public void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) throws SQLException {
        EPerson ePerson = getEPerson(context, jwtClaimsSet);

        context.setCurrentUser(ePerson);
    }

    public EPerson getEPerson(Context context, JWTClaimsSet jwtClaimsSet) throws SQLException {
        return ePersonService.find(context, getEPersonId(jwtClaimsSet));
    }

    private UUID getEPersonId(JWTClaimsSet jwtClaimsSet) {
        return UUID.fromString(jwtClaimsSet.getClaim(EPERSON_ID).toString());
    }
}
