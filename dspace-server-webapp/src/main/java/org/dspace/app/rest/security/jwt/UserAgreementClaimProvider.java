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
import org.apache.commons.lang3.BooleanUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link JWTClaimProvider} that handle the attributes related
 * to the user agreement.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class UserAgreementClaimProvider implements JWTClaimProvider {

    public static final String USER_AGREEMENT_ACCEPTED = "userAgreementAccepted";

    @Autowired
    private EPersonService ePersonService;

    @Override
    public String getKey() {
        return USER_AGREEMENT_ACCEPTED;
    }

    /**
     * Returns the current userAgreementAccepted value by searching for the
     * dspace.agreements.end-user metadata.
     */
    @Override
    public Object getValue(Context context, HttpServletRequest request) {
        EPerson user = context.getCurrentUser();
        try {
            // FIXME: necessary because some times the current user is obtained from the
            // hibernate cache and could have differents metadata. The whole try/catch with
            // its content should be removed after the problems with the hibernate cache
            // will be solved.
            context.uncacheEntity(user);
            user = ePersonService.find(context, user.getID());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String metadata = ePersonService.getMetadataFirstValue(user, "dspace", "agreements", "end-user", Item.ANY);
        String metadataIgnore = ePersonService.getMetadataFirstValue(user, "dspace", "agreements", "ignore", Item.ANY);
        // return true if the user can ignore the agreement or has accepted it
        return BooleanUtils.toBoolean(metadataIgnore) ? "true" : String.valueOf(BooleanUtils.toBoolean(metadata));
    }

    /**
     * Read the userAgreementAccepted attribute from the jwt token and add its value
     * to the request attributes.
     */
    @Override
    public void parseClaim(Context context, HttpServletRequest request, JWTClaimsSet jwtClaimsSet) throws SQLException {
        String userAgreementAccepted = jwtClaimsSet.getClaim(USER_AGREEMENT_ACCEPTED).toString();
        request.setAttribute(USER_AGREEMENT_ACCEPTED, userAgreementAccepted.equals("true"));
    }

}
