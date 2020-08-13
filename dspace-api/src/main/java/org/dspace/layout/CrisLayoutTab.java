/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.Cacheable;
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

import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_layout_tab")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutTab implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_tab_seq")
    @SequenceGenerator(name = "cris_layout_tab_seq", sequenceName = "cris_layout_tab_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private EntityType entity;
    @Column(name = "priority", nullable = false)
    private Integer priority;
    @Column(name = "shortname")
    private String shortName;
    @Column(name = "header")
    private String header;
    @Column(name = "security")
    private Integer security;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cris_layout_tab2securitymetadata",
        joinColumns = {@JoinColumn(name = "tab_id")},
        inverseJoinColumns = {@JoinColumn(name = "metadata_field_id")}
    )
    private Set<MetadataField> metadataSecurityFields;
    @OneToMany(
        mappedBy = "tab",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<CrisLayoutTab2Box> tab2box = new ArrayList<>();

    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EntityType getEntity() {
        return entity;
    }

    public void setEntity(EntityType entity) {
        this.entity = entity;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * This attribute is the label or the i18n key to use to present the section to the user
     * @return
     */
    public String getHeader() {
        return header;
    }

    /**
     * This attribute is the label or the i18n key to use to present the section to the user
     * @param header
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * This field manages the visibility of the tab
     * It can take the following values:
     * 0-PUBLIC
     * 1-ADMINISTRATOR
     * 2-OWNER ONLY
     * 3-OWNER & ADMINISTRATOR
     * 4-CUSTOM DATA
     * @return
     */
    public Integer getSecurity() {
        return security;
    }

    /**
     * This field manages the visibility of the tab
     * It can take the following values:
     * 0-PUBLIC
     * 1-ADMINISTRATOR
     * 2-OWNER ONLY
     * 3-OWNER & ADMINISTRATOR
     * 4-CUSTOM DATA
     * @param security {@link LayoutSecurity}
     */
    public void setSecurity(LayoutSecurity security) {
        this.security = security.getValue();
    }

    public Set<MetadataField> getMetadataSecurityFields() {
        return metadataSecurityFields;
    }

    public void setMetadataSecurityFields(Set<MetadataField> metadataFields) {
        this.metadataSecurityFields = metadataFields;
    }

    public void addMetadataSecurityFields(Set<MetadataField> metadataFields) {
        if (this.metadataSecurityFields == null) {
            this.metadataSecurityFields = new HashSet<>();
        }
        this.metadataSecurityFields.addAll(metadataFields);
    }

    public void addBox(CrisLayoutBox box) {
        this.addBox(box, null);
    }

    public void addBox(CrisLayoutBox box, Integer position) {
        if (this.tab2box.isEmpty()) {
            position = 0;
        } else if (position == null) {
            position = 0;
            for (Iterator<CrisLayoutTab2Box> it = this.tab2box.iterator();
                    it.hasNext(); ) {
                CrisLayoutTab2Box t2b = it.next();
                if (t2b.getPosition() >= position) {
                    position = t2b.getPosition() + 1;
                }
            }
        } else {
            int currentPosition = -1;
            for (Iterator<CrisLayoutTab2Box> it = this.tab2box.iterator();
                    it.hasNext(); ) {
                CrisLayoutTab2Box b2f = it.next();
                currentPosition = b2f.getPosition();
                if (currentPosition >= position ) {
                    b2f.setPosition(++currentPosition);
                }
            }
            if (position > ++currentPosition) {
                position = currentPosition;
            }
        }
        CrisLayoutTab2Box tab2box = new CrisLayoutTab2Box(this, box, position);
        this.tab2box.add(tab2box);
    }

    public void removeBox(int boxId) {
        boolean found = false;
        for (Iterator<CrisLayoutTab2Box> it = this.tab2box.iterator();
                it.hasNext();) {
            CrisLayoutTab2Box t2b = it.next();
            if (found) {
                t2b.setPosition(t2b.getPosition() - 1);
            }
            if (t2b.getTab().equals(this) &&
                    t2b.getId().getCrisLayoutBoxId().equals(boxId)) {
                it.remove();
                t2b.getBox().getTab2box().remove(t2b);
                t2b.setBox(null);
                t2b.setTab(null);
                found = true;
            }
        }
    }

    public void removeBox(CrisLayoutBox box) {
        boolean found = false;
        for (Iterator<CrisLayoutTab2Box> it = this.tab2box.iterator();
                it.hasNext();) {
            CrisLayoutTab2Box t2b = it.next();
            if (found) {
                t2b.setPosition(t2b.getPosition() - 1);
            }
            if (t2b.getTab().equals(this) && t2b.getBox().equals(box)) {
                it.remove();
                t2b.getBox().getTab2box().remove(t2b);
                t2b.setBox(null);
                t2b.setTab(null);
                found = true;
            }
        }
    }

    public List<CrisLayoutTab2Box> getTab2Box() {
        return tab2box;
    }

    public void setTab2Box(List<CrisLayoutTab2Box> tab2Box) {
        this.tab2box = tab2Box;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CrisLayoutTab other = (CrisLayoutTab) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (shortName == null) {
            if (other.shortName != null) {
                return false;
            }
        } else if (!shortName.equals(other.shortName)) {
            return false;
        }
        return true;
    }
}
