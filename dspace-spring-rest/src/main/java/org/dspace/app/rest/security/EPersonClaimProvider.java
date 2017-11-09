package org.dspace.app.rest.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

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
