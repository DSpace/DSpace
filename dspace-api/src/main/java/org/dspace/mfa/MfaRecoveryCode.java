/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA entity representing a single-use recovery code for MFA bypass.
 *
 * <p>Recovery codes provide an alternative authentication path when the user cannot
 * access their TOTP device. Each code can only be used once. The plaintext code is
 * never stored — only its SHA-256 hash is persisted for verification.</p>
 *
 * <p>A set of recovery codes is generated when MFA is first enabled and can be
 * regenerated on demand (invalidating all previous codes).</p>
 *
 * @author DSpace Contributors
 * @see Mfa
 * @see org.dspace.mfa.service.MfaService#generateRecoveryCodes
 */
@Entity
@Table(name = "mfa_recovery_code")
public class MfaRecoveryCode {

    /** Primary key / unique identifier for this recovery code record. */
    @Id
    @Column(name = "uuid")
    private UUID id;

    /** The MFA configuration this recovery code belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mfa_uuid", nullable = false)
    private Mfa mfa;

    /** SHA-256 hash of the plaintext recovery code. */
    @Column(name = "code_hash", nullable = false, length = 128)
    private String codeHash;

    /** Whether this recovery code has already been consumed. */
    @Column(name = "used", nullable = false)
    private boolean used = false;

    /**
     * Default no-arg constructor required by JPA.
     */
    protected MfaRecoveryCode() {}

    /**
     * Constructs a new recovery code entity.
     *
     * @param id       the unique identifier for this recovery code
     * @param mfa      the parent MFA configuration
     * @param codeHash the SHA-256 hex hash of the plaintext recovery code
     */
    public MfaRecoveryCode(UUID id, Mfa mfa, String codeHash) {
        this.id = id;
        this.mfa = mfa;
        this.codeHash = codeHash;
    }

    /**
     * Returns the unique identifier of this recovery code.
     *
     * @return the UUID primary key
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the parent MFA configuration.
     *
     * @return the associated {@link Mfa} entity
     */
    public Mfa getMfa() {
        return mfa;
    }

    /**
     * Returns the SHA-256 hash of the plaintext recovery code.
     *
     * @return the hex-encoded hash string
     */
    public String getCodeHash() {
        return codeHash;
    }

    /**
     * Returns whether this recovery code has been consumed.
     *
     * @return {@code true} if the code has been used, {@code false} otherwise
     */
    public boolean isUsed() {
        return used;
    }

    /**
     * Marks this recovery code as used or resets its used state.
     *
     * @param used {@code true} to mark as consumed, {@code false} to reset
     */
    public void setUsed(boolean used) {
        this.used = used;
    }
}
