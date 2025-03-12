/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.core.Context;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.SortNatural;

/**
 * Database entity representation of the registrationdata table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name = "registrationdata")
public class RegistrationData implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "registrationdata_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registrationdata_seq")
    @SequenceGenerator(name = "registrationdata_seq", sequenceName = "registrationdata_seq", allocationSize = 1)
    private Integer id;

    /**
     * Contains the email used to register the user.
     */
    @Column(name = "email", length = 64)
    private String email;

    /**
     * Contains the unique id generated fot the user.
     */
    @Column(name = "token", length = 48)
    private String token;

    /**
     * Expiration date of this registration data.
     */
    @Column(name = "expires")
    private Instant expires;

    /**
     * Metadata linked to this registration data
     */
    @SortNatural
    @OneToMany(
        fetch = FetchType.LAZY,
        mappedBy = "registrationData",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private SortedSet<RegistrationDataMetadata> metadata = new TreeSet<>();

    /**
     * External service used to register the user.
     * Allowed values are inside {@link RegistrationTypeEnum}
     */
    @Column(name = "registration_type")
    @Enumerated(EnumType.STRING)
    private RegistrationTypeEnum registrationType;

    /**
     * Contains the external id provided by the external service
     * accordingly to the registration type.
     */
    @Column(name = "net_id", length = 64)
    private final String netId;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.RegistrationDataService#create(Context)}
     */
    protected RegistrationData() {
        this(null);
    }

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.RegistrationDataService#create(Context, String)}
     */
    protected RegistrationData(String netId) {
        this.netId = netId;
    }

    public Integer getID() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }

    public Instant getExpires() {
        return expires;
    }

    void setExpires(Instant expires) {
        this.expires = expires;
    }

    public RegistrationTypeEnum getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(RegistrationTypeEnum registrationType) {
        this.registrationType = registrationType;
    }

    public SortedSet<RegistrationDataMetadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(SortedSet<RegistrationDataMetadata> metadata) {
        this.metadata = metadata;
    }

    public String getNetId() {
        return netId;
    }
}
