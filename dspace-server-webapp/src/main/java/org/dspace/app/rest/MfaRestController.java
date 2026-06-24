/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.security.DSpaceAuthentication;
import org.dspace.app.rest.security.RestAuthenticationService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.mfa.Mfa;
import org.dspace.mfa.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Multi-Factor Authentication setup and management.
 *
 * <p>Exposes endpoints under {@code /api/authn/mfa/} for users to manage their MFA
 * configuration and for administrators to manage MFA for other users.</p>
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code GET /status} — Check MFA status for current user</li>
 *   <li>{@code POST /setup} — Initialize TOTP secret and obtain provisioning URI</li>
 *   <li>{@code POST /verify-setup} — Confirm first TOTP code to activate MFA</li>
 *   <li>{@code POST /verify} — Verify TOTP or recovery code during login</li>
 *   <li>{@code POST /disable} — Deactivate MFA (requires valid TOTP code)</li>
 *   <li>{@code POST /recovery-codes} — Regenerate recovery codes</li>
 *   <li>{@code GET /admin/{uuid}/status} — Admin: get user MFA status</li>
 *   <li>{@code POST /admin/{uuid}/disable} — Admin: disable user MFA</li>
 * </ul>
 *
 * @author DSpace Contributors
 * @see MfaService
 * @see org.dspace.app.rest.security.MfaVerificationFilter
 * @see org.dspace.app.rest.security.jwt.MfaClaimProvider
 */
@RequestMapping(value = "/api/authn/mfa")
@RestController
public class MfaRestController {

    @Autowired
    private MfaService mfaService;

    @Autowired
    private RestAuthenticationService restAuthenticationService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * Returns the MFA status for the currently authenticated user.
     *
     * @param request the HTTP request (used to obtain the DSpace context)
     * @return a JSON map with keys: enabled, globallyEnabled, mandatory, setupRequired, remainingRecoveryCodes
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getStatus(HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();

        boolean globallyEnabled = mfaService.isGloballyEnabled();
        boolean enabled = globallyEnabled && mfaService.isMfaEnabled(context, eperson);
        int remainingCodes = enabled ? mfaService.countRemainingRecoveryCodes(context, eperson) : 0;
        boolean mandatory = mfaService.isMandatory();
        boolean setupRequired = mandatory && !enabled;

        return ResponseEntity.ok(Map.of(
            "enabled", enabled,
            "globallyEnabled", globallyEnabled,
            "mandatory", mandatory,
            "setupRequired", setupRequired,
            "remainingRecoveryCodes", remainingCodes
        ));
    }

    /**
     * Initializes MFA setup by generating a new TOTP secret and provisioning URI.
     *
     * @param request the HTTP request
     * @return JSON with {@code secret} and {@code provisioningUri}, or error if MFA is disabled/already enabled
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/setup", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> setup(HttpServletRequest request) throws SQLException {
        if (!mfaService.isGloballyEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "MFA is disabled system-wide"));
        }
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();

        try {
            Mfa mfa = mfaService.initSetup(context, eperson);
            String uri = mfaService.getProvisioningUri(mfa, eperson);
            context.commit();
            return ResponseEntity.ok(Map.of(
                "secret", mfa.getSecret(),
                "provisioningUri", uri
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifies the first TOTP code to confirm MFA setup and enable it.
     *
     * <p>On success, generates and returns recovery codes (shown only once).</p>
     *
     * @param request the HTTP request
     * @param body    JSON body with {@code code} field containing the 6-digit TOTP code
     * @return recovery codes on success, or error message on failure
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/verify-setup", method = RequestMethod.POST)
    public ResponseEntity<?> verifySetup(HttpServletRequest request,
                                         @RequestBody Map<String, String> body) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();
        String code = body.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        boolean valid = mfaService.verifyAndEnable(context, eperson, code.trim());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid TOTP code"));
        }

        // Generate recovery codes upon successful setup
        List<String> recoveryCodes = mfaService.generateRecoveryCodes(context, eperson);
        context.commit();

        return ResponseEntity.ok(Map.of("recoveryCodes", recoveryCodes));
    }

    /**
     * Verifies a TOTP code or recovery code during login (MFA challenge step).
     *
     * <p>On success, issues a new JWT with {@code mfa_verified=true}.</p>
     *
     * @param request  the HTTP request
     * @param response the HTTP response (used to set the new JWT)
     * @param body     JSON body with either {@code code} (TOTP) or {@code recoveryCode}
     * @return success status or error message
     * @throws Exception if an authentication or database error occurs
     */
    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public ResponseEntity<?> verify(HttpServletRequest request, HttpServletResponse response,
                                    @RequestBody Map<String, String> body) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();

        if (eperson == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentication required"));
        }

        String code = body.get("code");
        String recoveryCode = body.get("recoveryCode");

        boolean valid = false;
        if (code != null && !code.isBlank()) {
            valid = mfaService.verifyCode(context, eperson, code.trim());
        } else if (recoveryCode != null && !recoveryCode.isBlank()) {
            valid = mfaService.verifyRecoveryCode(context, eperson, recoveryCode.trim());
        }

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid code"));
        }

        // Mark MFA as verified so the MfaClaimProvider will set mfa_verified=true in the new token
        request.setAttribute("mfa.verified", true);
        context.commit();

        // Issue a new JWT with mfa_verified=true
        DSpaceAuthentication authentication = new DSpaceAuthentication(eperson.getEmail(), null);
        restAuthenticationService.addAuthenticationDataForUser(request, response, authentication, false);

        return ResponseEntity.ok(Map.of("status", "verified"));
    }

    /**
     * Disables MFA for the currently authenticated user.
     *
     * <p>Requires a valid TOTP code for confirmation.</p>
     *
     * @param request the HTTP request
     * @param body    JSON body with {@code code} field containing a valid TOTP code
     * @return success status or error message
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/disable", method = RequestMethod.POST)
    public ResponseEntity<?> disable(HttpServletRequest request,
                                     @RequestBody Map<String, String> body) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();
        String code = body.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        boolean valid = mfaService.verifyCode(context, eperson, code.trim());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid TOTP code"));
        }

        mfaService.disable(context, eperson);
        context.commit();
        return ResponseEntity.ok(Map.of("status", "disabled"));
    }

    /**
     * Regenerates recovery codes for the currently authenticated user.
     *
     * <p>Requires a valid TOTP code. All existing codes are invalidated.</p>
     *
     * @param request the HTTP request
     * @param body    JSON body with {@code code} field containing a valid TOTP code
     * @return JSON with new {@code recoveryCodes} list, or error message
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/recovery-codes", method = RequestMethod.POST)
    public ResponseEntity<?> regenerateRecoveryCodes(HttpServletRequest request,
                                                     @RequestBody Map<String, String> body) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPerson eperson = context.getCurrentUser();
        String code = body.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code is required"));
        }

        boolean valid = mfaService.verifyCode(context, eperson, code.trim());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid TOTP code"));
        }

        List<String> recoveryCodes = mfaService.generateRecoveryCodes(context, eperson);
        context.commit();
        return ResponseEntity.ok(Map.of("recoveryCodes", recoveryCodes));
    }

    /**
     * Returns the MFA status for a specific user (admin-only).
     *
     * @param request the HTTP request
     * @param uuid    the UUID of the target EPerson
     * @return JSON with MFA status, or 404 if user not found
     * @throws SQLException if a database access error occurs
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "/admin/{uuid}/status", method = RequestMethod.GET)
    public ResponseEntity<?> adminGetStatus(HttpServletRequest request,
                                            @PathVariable UUID uuid) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EPerson target = ePersonService.find(context, uuid);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        boolean enabled = mfaService.isMfaEnabled(context, target);
        int remainingCodes = enabled ? mfaService.countRemainingRecoveryCodes(context, target) : 0;
        return ResponseEntity.ok(Map.of("enabled", enabled, "remainingRecoveryCodes", remainingCodes));
    }

    /**
     * Disables MFA for a specific user (admin-only, no TOTP required).
     *
     * <p>Also invalidates the target user's session by clearing their session salt.</p>
     *
     * @param request the HTTP request
     * @param uuid    the UUID of the target EPerson
     * @return success status, or 404 if user not found
     * @throws SQLException                            if a database access error occurs
     * @throws org.dspace.authorize.AuthorizeException if authorization fails
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(value = "/admin/{uuid}/disable", method = RequestMethod.POST)
    public ResponseEntity<?> adminDisable(HttpServletRequest request,
                                          @PathVariable UUID uuid)
        throws SQLException, org.dspace.authorize.AuthorizeException {
        Context context = ContextUtil.obtainContext(request);
        EPerson target = ePersonService.find(context, uuid);
        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        if (!mfaService.isMfaEnabled(context, target)) {
            return ResponseEntity.ok(Map.of("status", "already_disabled"));
        }

        mfaService.disable(context, target);
        // Invalidate user's session
        target.setSessionSalt("");
        ePersonService.update(context, target);
        context.commit();
        return ResponseEntity.ok(Map.of("status", "disabled"));
    }
}
