/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
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
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_layout_box")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutBox implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_box_id_seq")
    @SequenceGenerator(name = "cris_layout_box_id_seq", sequenceName = "cris_layout_box_id_seq", allocationSize = 1)
    @Column(name = "id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_id")
    private EntityType entitytype;
    @Column(name = "type")
    private String type;
    @Column(name = "collapsed", nullable = false)
    private Boolean collapsed;
    @Column(name = "shortname")
    private String shortname;
    @Column(name = "header")
    private String header;
    @Column(name = "minor", nullable = false)
    private Boolean minor;
    @Column(name = "security")
    private Integer security;
    @Column(name = "style")
    private String style;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cris_layout_box2securitymetadata",
        joinColumns = {@JoinColumn(name = "box_id")},
        inverseJoinColumns = {@JoinColumn(name = "metadata_field_id")}
    )
    private Set<MetadataField> metadataSecurityFields;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "box", cascade = CascadeType.ALL)
    @OrderBy(value = "row, priority")
    private List<CrisLayoutField> layoutFields;
    @OneToMany(
            mappedBy = "box",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<CrisLayoutTab2Box> tab2box = new ArrayList<>();
    @Column(name = "clear")
    private Boolean clear;

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EntityType getEntitytype() {
        return entitytype;
    }

    public void setEntitytype(EntityType entitytype) {
        this.entitytype = entitytype;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getCollapsed() {
        return collapsed;
    }

    public void setCollapsed(Boolean collapsed) {
        this.collapsed = collapsed;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
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
     * This attribute is used to flag box that should be ignored in the determination of the tab visualization
     * @return
     */
    public Boolean getMinor() {
        return minor;
    }

    /**
     * This attribute is used to flag box that should be ignored in the determination of the tab visualization
     * @param minor
     */
    public void setMinor(Boolean minor) {
        this.minor = minor;
    }

    /**
     * This field manages the visibility of the box
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
     * This field manages the visibility of the box
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Set<MetadataField> getMetadataSecurityFields() {
        return metadataSecurityFields;
    }

    public void setMetadataSecurityFields(Set<MetadataField> metadataFields) {
        this.metadataSecurityFields = metadataFields;
    }

    public List<CrisLayoutField> getLayoutFields() {
        return layoutFields;
    }

    public void addLayoutField(CrisLayoutField layoutField) {
        if (this.layoutFields == null) {
            this.layoutFields = new ArrayList<>();
        }
        this.layoutFields.add(layoutField);
    }

    public void setLayoutFields(List<CrisLayoutField> layoutFields) {
        this.layoutFields = layoutFields;
    }

    public Boolean getClear() {
        return clear;
    }

    public void setClear(Boolean clear) {
        this.clear = clear;
    }

    public List<CrisLayoutTab2Box> getTab2box() {
        return tab2box;
    }

    public void setTab2box(List<CrisLayoutTab2Box> tab2box) {
        this.tab2box = tab2box;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((shortname == null) ? 0 : shortname.hashCode());
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
        CrisLayoutBox other = (CrisLayoutBox) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (shortname == null) {
            if (other.shortname != null) {
                return false;
            }
        } else if (!shortname.equals(other.shortname)) {
            return false;
        }
        return true;
    }

}
