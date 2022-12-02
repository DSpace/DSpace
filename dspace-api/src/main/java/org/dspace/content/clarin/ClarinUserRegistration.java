/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.dspace.core.ReloadableEntity;

@Entity
@Table(name = "user_registration")
public class ClarinUserRegistration implements ReloadableEntity<Integer> {

    private static Logger log = Logger.getLogger(ClarinUserRegistration.class);

    @Id
    @Column(name = "user_registration_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "user_registration_user_registration_id_seq")
    @SequenceGenerator(name = "user_registration_user_registration_id_seq",
            sequenceName = "user_registration_user_registration_id_seq",
            allocationSize = 1)
    protected Integer id;

    @Column(name = "eperson_id")
    private UUID ePersonID = null;

    @Column(name = "email")
    private String email = null;

    @Column(name = "organization")
    private String organization = null;

    @Column(name = "confirmation")
    private boolean confirmation = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "eperson", cascade = CascadeType.PERSIST)
    private List<ClarinLicense> clarinLicenses = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "userRegistration", cascade = CascadeType.PERSIST)
    private List<ClarinLicenseResourceUserAllowance> licenseResourceUserAllowances = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "eperson", cascade = CascadeType.PERSIST)
    private List<ClarinUserMetadata> userMetadata = new ArrayList<>();

    public ClarinUserRegistration() {
    }

    public UUID getPersonID() {
        return ePersonID;
    }

    public void setPersonID(UUID ePersonID) {
        this.ePersonID = ePersonID;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getID() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public boolean isConfirmation() {
        return confirmation;
    }

    public void setConfirmation(boolean confirmation) {
        this.confirmation = confirmation;
    }

    public List<ClarinLicense> getClarinLicenses() {
        return clarinLicenses;
    }

    public List<ClarinLicenseResourceUserAllowance> getLicenseResourceUserAllowances() {
        return licenseResourceUserAllowances;
    }

    public void setClarinLicenses(List<ClarinLicense> clarinLicenses) {
        this.clarinLicenses = clarinLicenses;
    }

    public void setLicenseResourceUserAllowances(List<ClarinLicenseResourceUserAllowance>
                                                         licenseResourceUserAllowances) {
        this.licenseResourceUserAllowances = licenseResourceUserAllowances;
    }

    public List<ClarinUserMetadata> getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(List<ClarinUserMetadata> userMetadata) {
        this.userMetadata = userMetadata;
    }
}
