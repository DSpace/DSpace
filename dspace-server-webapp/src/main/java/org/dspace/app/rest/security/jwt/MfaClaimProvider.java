/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import java.sql.SQLException;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.mfa.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JWT claim provider that manages the {@code mfa_verified} claim in authentication tokens.
 *
 * <p>This component participates in JWT token generation and parsing to enforce MFA
 * verification state. The claim determines whether a user has completed their MFA
 * challenge for the current session:</p>
 *
 * <ul>
 *   <li>{@code mfa_verified=true} — User has full API access</li>
 *   <li>{@code mfa_verified=false} — User must complete MFA verification before
 *       accessing protected endpoints (enforced by {@link org.dspace.app.rest.security.MfaVerificationFilter})</li>
 * </ul>
 *
 * <h3>Claim value logic during token generation:</h3>
 * <ol>
 *   <li>If MFA is globally disabled → {@code true}</li>
 *   <li>If the request has {@code mfa.verified} attribute set → {@code true} (just verified)</li>
 *   <li>If user has MFA enabled → {@code false} (must verify)</li>
 *   <li>If MFA is mandatory but user hasn't enrolled → {@code false} (must set up)</li>
 *   <li>Otherwise → {@code true} (no MFA requirement)</li>
 * </ol>
 *
 * @author DSpace Contributors
 * @see org.dspace.app.rest.security.MfaVerificationFilter
 * @see org.dspace.app.rest.MfaRestController
 */
@Component
public class MfaClaimProvider implements JWTClaimProvider {

    /** The JWT claim key used to store the MFA verification state. */
    public static final String MFA_VERIFIED = "mfa_verified";

    @Autowired
    private MfaService mfaService;

    /**
     * Returns the JWT claim key managed by this provider.
     *
     * @return the string {@code "mfa_verified"}
     */
    @Override
    public String getKey() {
        return MFA_VERIFIED;
    }

    /**
     * Computes the value of the {@code mfa_verified} claim for token generation.
     *
     * @param context the DSpace context with the current user
     * @param request the HTTP request (checked for {@code mfa.verified} attribute)
     * @return {@code true} if the user has verified MFA or MFA is not required,
     *         {@code false} if MFA verification is pending
     */
    @Override
    public Object getValue(Context context, HttpServletRequest request) {
        // If MFA is globally disabled, always mark as verified
        if (!mfaService.isGloballyEnabled()) {
            return true;
        }

        // If MFA was just verified in this request (set by MfaRestController)
        Boolean verified = (Boolean) request.getAttribute("mfa.verified");
        if (Boolean.TRUE.equals(verified)) {
            return true;
        }

        try {
            EPerson eperson = context.getCurrentUser();
            if (eperson == null) {
                return true;
            }
            boolean userHasMfa = mfaService.isMfaEnabled(context, eperson);

            // If user has MFA enabled, token starts as unverified
            if (userHasMfa) {
                return false;
            }

            // If MFA is mandatory but user hasn't enrolled yet, also block access
            if (mfaService.isMandatory()) {
                return false;
            }

            // User doesn't have MFA and it's not mandatory — fully verified
            return true;
        } catch (SQLException e) {
            return true;
        }
    }

    /**
     * Parses the {@code mfa_verified} claim from an incoming JWT and stores it
     * as a request attribute for use by downstream filters.
     *
     * @param context      the DSpace context
     * @param request      the HTTP request to set the attribute on
     * @param jwtClaimsSet the parsed JWT claims
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void parseClaim(Context context, HttpServletRequest request,
                           JWTClaimsSet jwtClaimsSet) throws SQLException {
        Object claim = jwtClaimsSet.getClaim(MFA_VERIFIED);
        if (claim != null) {
            request.setAttribute(MFA_VERIFIED, Boolean.valueOf(claim.toString()));
        }
    }
}
