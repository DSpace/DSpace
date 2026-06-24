/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.service.impl;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.apache.commons.codec.digest.DigestUtils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.mfa.Mfa;
import org.dspace.mfa.MfaRecoveryCode;
import org.dspace.mfa.MfaType;
import org.dspace.mfa.dao.MfaDAO;
import org.dspace.mfa.dao.MfaRecoveryCodeDAO;
import org.dspace.mfa.service.MfaService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MfaService} using TOTP (RFC 6238) for code generation and verification.
 *
 * <p>This service manages the full MFA lifecycle: setup initiation, TOTP verification,
 * enabling/disabling MFA, and recovery code management. It delegates persistence to
 * {@link MfaDAO} and {@link MfaRecoveryCodeDAO}.</p>
 *
 * <p>Configuration properties (from DSpace configuration):</p>
 * <ul>
 *   <li>{@code mfa.totp.enabled} — whether MFA is available system-wide (default: true)</li>
 *   <li>{@code mfa.totp.mandatory} — whether MFA is required for all users (default: false)</li>
 *   <li>{@code mfa.totp.issuer} — the issuer label shown in authenticator apps (default: "DSpace")</li>
 * </ul>
 *
 * <p>Recovery codes are 8-character alphanumeric strings stored as SHA-256 hashes.
 * A set of 8 codes is generated per user.</p>
 *
 * @author DSpace Contributors
 * @see MfaService
 */
public class MfaServiceImpl implements MfaService {

    /** Number of recovery codes generated per user. */
    private static final int RECOVERY_CODE_COUNT = 8;

    /** Character length of each generated recovery code. */
    private static final int RECOVERY_CODE_LENGTH = 8;

    /** Configuration key controlling whether MFA is globally enabled. */
    private static final String ENABLED_CONFIG_KEY = "mfa.totp.enabled";

    /** Configuration key controlling whether MFA is mandatory. */
    private static final String MANDATORY_CONFIG_KEY = "mfa.totp.mandatory";

    /** Configuration key for the TOTP issuer label. */
    private static final String ISSUER_CONFIG_KEY = "mfa.totp.issuer";

    /** Default issuer label used if none is configured. */
    private static final String DEFAULT_ISSUER = "DSpace";

    @Autowired
    private MfaDAO mfaDAO;

    @Autowired
    private MfaRecoveryCodeDAO recoveryCodeDAO;

    @Autowired
    private ConfigurationService configurationService;

    /** Generates Base32-encoded secrets for TOTP setup. */
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator(32);

    /** Verifies TOTP codes against a secret using SHA1, 6 digits, 30-second period. */
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(
        new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
        new SystemTimeProvider()
    );

    /** Cryptographically secure random number generator for recovery code generation. */
    private final SecureRandom secureRandom = new SecureRandom();

    /** {@inheritDoc} */
    @Override
    public boolean isGloballyEnabled() {
        return configurationService.getBooleanProperty(ENABLED_CONFIG_KEY, true);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMandatory() {
        return isGloballyEnabled() && configurationService.getBooleanProperty(MANDATORY_CONFIG_KEY, false);
    }

    /** {@inheritDoc} */
    @Override
    public Mfa initSetup(Context context, EPerson eperson) throws SQLException {
        Mfa existing = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (existing != null && existing.isEnabled()) {
            throw new IllegalStateException("MFA is already enabled. Disable it first.");
        }

        String secret = secretGenerator.generate();

        if (existing != null) {
            // Reuse existing non-enabled record with a fresh secret
            existing.setSecret(secret);
            mfaDAO.save(context, existing);
            return existing;
        }

        Mfa mfa = new Mfa(UUID.randomUUID(), eperson, MfaType.TOTP, secret);
        mfaDAO.create(context, mfa);
        return mfa;
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyAndEnable(Context context, EPerson eperson, String code) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa == null) {
            return false;
        }
        if (!codeVerifier.isValidCode(mfa.getSecret(), code)) {
            return false;
        }
        mfa.setEnabled(true);
        mfaDAO.save(context, mfa);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyCode(Context context, EPerson eperson, String code) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa == null || !mfa.isEnabled()) {
            return false;
        }
        return codeVerifier.isValidCode(mfa.getSecret(), code);
    }

    /** {@inheritDoc} */
    @Override
    public boolean verifyRecoveryCode(Context context, EPerson eperson, String code) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa == null || !mfa.isEnabled()) {
            return false;
        }
        List<MfaRecoveryCode> unused = recoveryCodeDAO.findUnusedByMfa(context, mfa.getId());
        for (MfaRecoveryCode rc : unused) {
            if (hashCode(code.trim().toLowerCase()).equals(rc.getCodeHash())) {
                rc.setUsed(true);
                recoveryCodeDAO.save(context, rc);
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMfaEnabled(Context context, EPerson eperson) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        return mfa != null && mfa.isEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public Mfa findByEPerson(Context context, EPerson eperson) throws SQLException {
        return mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
    }

    /** {@inheritDoc} */
    @Override
    public void disable(Context context, EPerson eperson) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa != null) {
            recoveryCodeDAO.deleteByMfa(context, mfa.getId());
            mfaDAO.delete(context, mfa);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> generateRecoveryCodes(Context context, EPerson eperson) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa == null) {
            throw new IllegalStateException("MFA is not configured for this user.");
        }
        // Delete existing codes
        recoveryCodeDAO.deleteByMfa(context, mfa.getId());

        List<String> plaintextCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = generateRandomCode();
            plaintextCodes.add(code);
            String hash = hashCode(code);
            MfaRecoveryCode rc = new MfaRecoveryCode(UUID.randomUUID(), mfa, hash);
            recoveryCodeDAO.create(context, rc);
        }
        return plaintextCodes;
    }

    /** {@inheritDoc} */
    @Override
    public int countRemainingRecoveryCodes(Context context, EPerson eperson) throws SQLException {
        Mfa mfa = mfaDAO.findByEPersonAndType(context, eperson, MfaType.TOTP);
        if (mfa == null) {
            return 0;
        }
        return recoveryCodeDAO.findUnusedByMfa(context, mfa.getId()).size();
    }

    /** {@inheritDoc} */
    @Override
    public String getProvisioningUri(Mfa mfa, EPerson eperson) {
        String issuer = configurationService.getProperty(ISSUER_CONFIG_KEY, DEFAULT_ISSUER);
        // otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
            issuer, eperson.getEmail(), mfa.getSecret(), issuer);
    }

    /**
     * Generates a random alphanumeric recovery code.
     *
     * @return a lowercase alphanumeric string of length {@link #RECOVERY_CODE_LENGTH}
     */
    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Computes the SHA-256 hash of a recovery code for secure storage.
     *
     * @param code the plaintext recovery code
     * @return the hex-encoded SHA-256 hash
     */
    private String hashCode(String code) {
        return DigestUtils.sha256Hex(code);
    }
}
