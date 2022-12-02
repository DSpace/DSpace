/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.clarin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ReloadableEntity;


/**
 * Class representing a clarin license in DSpace.
 * Clarin License is license for the bitstreams of the item. The item could have only one type of the Clarin License.
 * The Clarin License is selected in the submission process.
 * Admin could manage Clarin Licenses in the License Administration page.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Entity
@Table(name = "license_definition")
public class ClarinLicense implements ReloadableEntity<Integer> {

    private static Logger log = Logger.getLogger(ClarinLicense.class);

    /**
     * Required info key word.
     */
    public static final String SEND_TOKEN = "SEND_TOKEN";

    @Id
    @Column(name = "license_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "license_definition_license_id_seq")
    @SequenceGenerator(name = "license_definition_license_id_seq", sequenceName = "license_definition_license_id_seq",
            allocationSize = 1)
    private Integer id;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinTable(
            name = "license_label_extended_mapping",
            joinColumns = @JoinColumn(name = "license_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    Set<ClarinLicenseLabel> clarinLicenseLabels = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "license", cascade = CascadeType.PERSIST)
    private List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "user_registration_id")
    private ClarinUserRegistration eperson;

    @Column(name = "name")
    private String name = null;

    @Column(name = "definition")
    private String definition = null;

    @Column(name = "confirmation")
    private Integer confirmation = 0;

    @Column(name = "required_info")
    private String requiredInfo = null;

    public ClarinLicense() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public Integer getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(Integer confirmation) {
        this.confirmation = confirmation;
    }

    public String getRequiredInfo() {
        return requiredInfo;
    }

    public void setRequiredInfo(String requiredInfo) {
        this.requiredInfo = requiredInfo;
    }

    public List<ClarinLicenseLabel> getLicenseLabels() {
        ClarinLicenseLabel[] output = clarinLicenseLabels.toArray(new ClarinLicenseLabel[] {});
        return Arrays.asList(output);
    }

    public void setLicenseLabels(Set<ClarinLicenseLabel> clarinLicenseLabels) {
        this.clarinLicenseLabels = clarinLicenseLabels;
    }

    public List<ClarinLicenseResourceMapping> getClarinLicenseResourceMappings() {
        return clarinLicenseResourceMappings;
    }

    /**
     * The bitstream is not removed from the database after deleting the item, but is set as `deleted`.
     * Do not count deleted bitstreams for the clarin license.
     * @return count of the non deleted bitstream assigned to the current clarin license.
     */
    public int getNonDeletedBitstreams() {
        int counter = 0;

        for (ClarinLicenseResourceMapping clrm : clarinLicenseResourceMappings) {
            Bitstream bitstream = clrm.getBitstream();
            try {
                if (bitstream.isDeleted()) {
                    continue;
                }
                counter++;
            } catch (SQLException e) {
                log.error("Cannot find out if the bitstream: " + bitstream.getID() + " is deleted.");
            }
        }
        return counter;
    }

    public ClarinLicenseLabel getNonExtendedClarinLicenseLabel() {
        for (ClarinLicenseLabel cll : getLicenseLabels()) {
            if (!cll.isExtended()) {
                return cll;
            }
        }
        return null;
    }

    @Override
    public Integer getID() {
        return id;
    }

    public Set<ClarinLicenseLabel> getClarinLicenseLabels() {
        return clarinLicenseLabels;
    }

    public void setClarinLicenseLabels(Set<ClarinLicenseLabel> clarinLicenseLabels) {
        this.clarinLicenseLabels = clarinLicenseLabels;
    }

    public ClarinUserRegistration getEperson() {
        return eperson;
    }

    public void setEperson(ClarinUserRegistration eperson) {
        this.eperson = eperson;
    }
}
