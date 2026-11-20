/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.mfa.Mfa;

/**
 * Service interface for Multi-Factor Authentication (MFA) operations.
 *
 * <p>Provides the business logic layer for managing MFA lifecycle including setup,
 * verification, disabling, and recovery code management. All operations are scoped
 * to a specific {@link EPerson} and require a valid DSpace {@link Context}.</p>
 *
 * <p>The service respects system-wide configuration properties to determine whether
 * MFA is globally enabled and/or mandatory for all users.</p>
 *
 * @author DSpace Contributors
 * @see org.dspace.mfa.service.impl.MfaServiceImpl
 * @see Mfa
 */
public interface MfaService {

    /**
     * Checks if MFA is globally enabled in the system configuration.
     *
     * <p>When disabled, all MFA enforcement is bypassed and users cannot set up MFA.</p>
     *
     * @return {@code true} if MFA is enabled system-wide, {@code false} otherwise
     */
    boolean isGloballyEnabled();

    /**
     * Checks if MFA is mandatory for all users.
     *
     * <p>When mandatory, users without MFA configured will be forced to set it up
     * before accessing the system. Only effective if MFA is also globally enabled.</p>
     *
     * @return {@code true} if MFA is mandatory and globally enabled, {@code false} otherwise
     */
    boolean isMandatory();

    /**
     * Initializes MFA setup for a user by generating a new TOTP secret.
     *
     * <p>If the user already has an enabled MFA configuration, this method throws
     * {@link IllegalStateException}. If a non-enabled (pending) record exists, its
     * secret is refreshed.</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user to initialize MFA for
     * @return the created or updated {@link Mfa} entity containing the new secret
     * @throws SQLException          if a database access error occurs
     * @throws IllegalStateException if MFA is already enabled for the user
     */
    Mfa initSetup(Context context, EPerson eperson) throws SQLException;

    /**
     * Verifies a TOTP code and enables MFA if this is the first successful verification.
     *
     * <p>Used during the setup confirmation flow. If the code is valid, the MFA
     * record is transitioned from disabled to enabled state.</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user performing setup verification
     * @param code    the 6-digit TOTP code to verify
     * @return {@code true} if the code is valid and MFA has been enabled, {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean verifyAndEnable(Context context, EPerson eperson, String code) throws SQLException;

    /**
     * Verifies a TOTP code during login for a user with MFA already enabled.
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user attempting to verify their TOTP code
     * @param code    the 6-digit TOTP code to verify
     * @return {@code true} if the code is valid, {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean verifyCode(Context context, EPerson eperson, String code) throws SQLException;

    /**
     * Verifies a recovery code during login as an alternative to TOTP.
     *
     * <p>If the code matches an unused recovery code, it is marked as consumed
     * and cannot be reused.</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user attempting recovery code verification
     * @param code    the plaintext recovery code to verify
     * @return {@code true} if the recovery code is valid and has been consumed,
     *         {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean verifyRecoveryCode(Context context, EPerson eperson, String code) throws SQLException;

    /**
     * Checks whether a user has MFA enabled and active.
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user to check
     * @return {@code true} if the user has an active MFA configuration, {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean isMfaEnabled(Context context, EPerson eperson) throws SQLException;

    /**
     * Retrieves the MFA configuration for a user.
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user whose MFA record to retrieve
     * @return the {@link Mfa} entity, or {@code null} if no MFA record exists
     * @throws SQLException if a database access error occurs
     */
    Mfa findByEPerson(Context context, EPerson eperson) throws SQLException;

    /**
     * Disables MFA for a user, removing the secret and all associated recovery codes.
     *
     * <p>After calling this method, the user will no longer be required to provide
     * a TOTP code during login (unless MFA is mandatory and they re-enroll).</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user whose MFA should be disabled
     * @throws SQLException if a database access error occurs
     */
    void disable(Context context, EPerson eperson) throws SQLException;

    /**
     * Generates a new set of recovery codes, replacing any previously existing codes.
     *
     * <p>The returned plaintext codes must be displayed to the user exactly once.
     * Only their hashes are stored in the database.</p>
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user to generate recovery codes for
     * @return a list of plaintext recovery codes (caller must present these to the user)
     * @throws SQLException          if a database access error occurs
     * @throws IllegalStateException if MFA is not configured for the user
     */
    List<String> generateRecoveryCodes(Context context, EPerson eperson) throws SQLException;

    /**
     * Counts the number of remaining unused recovery codes for a user.
     *
     * @param context the DSpace context providing the database session
     * @param eperson the user whose remaining codes to count
     * @return the number of unused recovery codes, or 0 if MFA is not configured
     * @throws SQLException if a database access error occurs
     */
    int countRemainingRecoveryCodes(Context context, EPerson eperson) throws SQLException;

    /**
     * Builds the TOTP provisioning URI for QR code generation.
     *
     * <p>The returned URI follows the {@code otpauth://} scheme and can be encoded
     * as a QR code for scanning by authenticator apps.</p>
     *
     * @param mfa     the MFA entity containing the secret
     * @param eperson the user (used for the account label in the URI)
     * @return the provisioning URI string in {@code otpauth://totp/} format
     */
    String getProvisioningUri(Mfa mfa, EPerson eperson);
}
