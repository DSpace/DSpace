package org.dspace.content.clarin;

import org.dspace.core.ReloadableEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "license_label")
public class ClarinLicenseLabel implements ReloadableEntity<Integer> {

    @Id
    @Column(name="label_id")
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

    public Integer getId() {
        return id;
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
