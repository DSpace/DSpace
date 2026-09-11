/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.dspace.eperson.EPerson;

/**
 * JPA entity representing a multi-factor authentication (MFA) configuration for a DSpace user.
 *
 * <p>Each {@link EPerson} may have at most one active MFA record per {@link MfaType}. The entity
 * stores the shared secret used for TOTP code generation and tracks whether MFA has been
 * confirmed and enabled by the user.</p>
 *
 * <p>Lifecycle: An MFA record is created in a disabled state during setup initiation
 * ({@link org.dspace.mfa.service.MfaService#initSetup}). Once the user verifies their first
 * TOTP code, the record transitions to enabled. Disabling MFA deletes the record entirely.</p>
 *
 * @author DSpace Contributors
 * @see MfaType
 * @see MfaRecoveryCode
 * @see org.dspace.mfa.service.MfaService
 */
@Entity
@Table(name = "mfa")
public class Mfa {

    /** Primary key / unique identifier for this MFA configuration. */
    @Id
    @Column(name = "uuid")
    private UUID id;

    /** The user to whom this MFA configuration belongs. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eperson_uuid", nullable = false)
    private EPerson eperson;

    /** The type of MFA mechanism (e.g., TOTP). */
    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_type", nullable = false, length = 32)
    private MfaType mfaType = MfaType.TOTP;

    /** The shared secret used for TOTP code generation (Base32-encoded). */
    @Column(name = "secret", nullable = false, length = 128)
    private String secret;

    /** Whether the user has confirmed and activated MFA. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;

    /** Timestamp when this MFA record was created. */
    @Column(name = "created_on", nullable = false)
    private Instant createdOn = Instant.now();

    /**
     * Default no-arg constructor required by JPA.
     */
    protected Mfa() {}

    /**
     * Constructs a new MFA entity with the specified parameters.
     *
     * @param id      the unique identifier for this MFA record
     * @param eperson the user to associate MFA with
     * @param mfaType the type of MFA mechanism
     * @param secret  the Base32-encoded shared secret for code generation
     */
    public Mfa(UUID id, EPerson eperson, MfaType mfaType, String secret) {
        this.id = id;
        this.eperson = eperson;
        this.mfaType = mfaType;
        this.secret = secret;
    }

    /**
     * Returns the unique identifier of this MFA record.
     *
     * @return the UUID primary key
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the EPerson (user) associated with this MFA configuration.
     *
     * @return the owning EPerson
     */
    public EPerson getEperson() {
        return eperson;
    }

    /**
     * Returns the MFA mechanism type.
     *
     * @return the MFA type (e.g., {@link MfaType#TOTP})
     */
    public MfaType getMfaType() {
        return mfaType;
    }

    /**
     * Returns the Base32-encoded shared secret used for TOTP code generation.
     *
     * @return the TOTP secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Updates the shared secret. Used when re-initializing setup on an existing non-enabled record.
     *
     * @param secret the new Base32-encoded secret
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Returns whether this MFA configuration is active and enforced.
     *
     * @return {@code true} if MFA is enabled for the user, {@code false} if still in setup
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled state of this MFA configuration.
     *
     * @param enabled {@code true} to activate MFA, {@code false} to deactivate
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the timestamp when this MFA record was first created.
     *
     * @return the creation instant
     */
    public Instant getCreatedOn() {
        return createdOn;
    }
}
