/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.security.jwt.MfaClaimProvider;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that enforces MFA verification before granting API access.
 *
 * <p>This filter inspects the {@code mfa_verified} request attribute (set by
 * {@link MfaClaimProvider} during JWT parsing) and blocks requests from users
 * who have not yet completed their MFA challenge.</p>
 *
 * <p>When a request is blocked, a 403 Forbidden response is returned with a JSON
 * body indicating that MFA verification is required. The frontend uses this signal
 * to redirect the user to the MFA verification screen.</p>
 *
 * <h3>Exempt paths:</h3>
 * <p>The following endpoints are allowed through regardless of MFA state to enable
 * the verification flow itself:</p>
 * <ul>
 *   <li>{@code /api/authn/mfa/verify} — TOTP/recovery code verification</li>
 *   <li>{@code /api/authn/mfa/setup} — MFA setup initiation</li>
 *   <li>{@code /api/authn/mfa/verify-setup} — Setup confirmation</li>
 *   <li>{@code /api/authn/mfa/status} — MFA status check</li>
 *   <li>{@code /api/authn/logout} — Session logout</li>
 *   <li>{@code /api/authn/status} — Authentication status</li>
 * </ul>
 *
 * @author DSpace Contributors
 * @see MfaClaimProvider
 * @see org.dspace.app.rest.MfaRestController
 */
public class MfaVerificationFilter extends OncePerRequestFilter {

    /** Path for TOTP/recovery code verification during login. */
    private static final String MFA_VERIFY_PATH = "/api/authn/mfa/verify";

    /** Path for MFA setup initiation. */
    private static final String MFA_SETUP_PATH = "/api/authn/mfa/setup";

    /** Path for MFA setup confirmation. */
    private static final String MFA_VERIFY_SETUP_PATH = "/api/authn/mfa/verify-setup";

    /** Path for MFA status check. */
    private static final String MFA_STATUS_PATH = "/api/authn/mfa/status";

    /** Path for session logout. */
    private static final String LOGOUT_PATH = "/api/authn/logout";

    /** Path for authentication status check. */
    private static final String STATUS_PATH = "/api/authn/status";

    /**
     * Filters incoming requests, blocking those with {@code mfa_verified=false}
     * unless they target an exempt path.
     *
     * @param request     the HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain to continue processing
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Allow MFA-related, logout, and status endpoints through regardless
        String path = request.getRequestURI();
        if (path.contains(MFA_VERIFY_PATH) || path.contains(MFA_SETUP_PATH)
            || path.contains(MFA_VERIFY_SETUP_PATH) || path.contains(MFA_STATUS_PATH)
            || path.contains(LOGOUT_PATH) || path.contains(STATUS_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if MFA verification is pending
        Object mfaVerified = request.getAttribute(MfaClaimProvider.MFA_VERIFIED);
        if (Boolean.FALSE.equals(mfaVerified)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"MFA verification required\",\"mfa_required\":true}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
