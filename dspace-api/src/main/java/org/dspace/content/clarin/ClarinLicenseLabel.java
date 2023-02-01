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
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.core.ReloadableEntity;

/**
 * Class representing a clarin license label of the clarin license. The clarin license could have one
 * non-extended license label and multiple extended license labels.
 * The license label could be defined in the License Administration table.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Entity
@Table(name = "license_label")
public class ClarinLicenseLabel implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "label_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "license_label_label_id_seq")
    @SequenceGenerator(name = "license_label_label_id_seq", sequenceName = "license_label_label_id_seq",
            allocationSize = 1)
    private Integer id;

    @Column(name = "label")
    private String label = null;

    @Column(name = "title")
    private String title = null;

    @Column(name = "is_extended")
    private boolean isExtended = false;

    @Column(name = "icon")
    private byte[] icon = null;

    @ManyToMany(mappedBy = "clarinLicenseLabels")
    List<ClarinLicense> licenses = new ArrayList<>();

    public ClarinLicenseLabel() {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    public List<ClarinLicense> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<ClarinLicense> licenses) {
        this.licenses = licenses;
    }

    public void addLicense(ClarinLicense license) {
        if (Objects.isNull(this.licenses)) {
            this.licenses = new ArrayList<>();
        }
        this.licenses.add(license);
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    @Override
    public Integer getID() {
        return id;
    }
}
