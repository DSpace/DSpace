/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa.service.impl;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
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

    /** TOTP time step duration (30 seconds per RFC 6238). */
    private static final Duration TIME_STEP = Duration.ofSeconds(30);

    /** HMAC algorithm used for TOTP generation. */
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /** Number of TOTP digits. */
    private static final int CODE_DIGITS = 6;

    @Autowired
    private MfaDAO mfaDAO;

    @Autowired
    private MfaRecoveryCodeDAO recoveryCodeDAO;

    @Autowired
    private ConfigurationService configurationService;

    /** Base32 codec for encoding/decoding TOTP secrets. */
    private final Base32 base32 = new Base32();

    /** TOTP generator using SHA1, 6 digits, 30-second period. */
    private final TimeBasedOneTimePasswordGenerator totpGenerator;

    /** Cryptographically secure random number generator for recovery code generation. */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Constructs the service and initializes the TOTP generator.
     *
     * @throws NoSuchAlgorithmException if HmacSHA1 is not available
     */
    public MfaServiceImpl() throws NoSuchAlgorithmException {
        this.totpGenerator = new TimeBasedOneTimePasswordGenerator(TIME_STEP, CODE_DIGITS, HMAC_ALGORITHM);
    }

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

        String secret = generateBase32Secret();

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
        if (!isValidCode(mfa.getSecret(), code)) {
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
        return isValidCode(mfa.getSecret(), code);
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

    /**
     * Generates a new TOTP secret and returns it as a Base32-encoded string.
     *
     * @return Base32-encoded secret suitable for storage and provisioning URIs
     */
    private String generateBase32Secret() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(HMAC_ALGORITHM);
            keyGenerator.init(160); // 20 bytes = 160 bits, standard for HmacSHA1
            SecretKey key = keyGenerator.generateKey();
            return base32.encodeToString(key.getEncoded()).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA1 algorithm not available", e);
        }
    }

    /**
     * Converts a Base32-encoded secret string into a {@link SecretKey}.
     *
     * @param base32Secret the Base32-encoded secret from the database
     * @return a SecretKey suitable for TOTP generation
     */
    private SecretKey secretKeyFromBase32(String base32Secret) {
        byte[] decoded = base32.decode(base32Secret);
        return new SecretKeySpec(decoded, HMAC_ALGORITHM);
    }

    /**
     * Validates a TOTP code against the stored secret, allowing a window of +/- 1 time step
     * to account for clock drift between server and client.
     *
     * @param base32Secret the Base32-encoded secret from the database
     * @param code the TOTP code provided by the user
     * @return true if the code matches the current or adjacent time steps
     */
    private boolean isValidCode(String base32Secret, String code) {
        SecretKey key = secretKeyFromBase32(base32Secret);
        Instant now = Instant.now();
        try {
            for (int i = -1; i <= 1; i++) {
                Instant timestamp = now.plus(TIME_STEP.multipliedBy(i));
                String expected = totpGenerator.generateOneTimePasswordString(key, timestamp);
                if (expected.equals(code)) {
                    return true;
                }
            }
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Invalid TOTP secret key", e);
        }
        return false;
    }
}
