/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
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
//    @Column(name = "priority", nullable = false)
//    private Integer priority;
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
        name = "cris_layout_box2securityfield",
        joinColumns = {@JoinColumn(name = "box_id")},
        inverseJoinColumns = {@JoinColumn(name = "authorized_field_id")}
    )
    private Set<MetadataField> metadataSecurityFields;
    @OneToMany(
        mappedBy = "box",
        cascade = CascadeType.ALL
    )
    private List<CrisLayoutBox2Field> box2field = new ArrayList<>();
    @OneToMany(
            mappedBy = "box",
            cascade = CascadeType.ALL
    )
    private List<CrisLayoutTab2Box> tab2box = new ArrayList<>();
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(
//            name = "cris_layout_tab2box",
//            joinColumns = {@JoinColumn(name = "cris_layout_box_id")},
//            inverseJoinColumns = {@JoinColumn(name = "cris_layout_tab_id")}
//        )
//    private Set<CrisLayoutTab> tabs;
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

//    public Integer getPriority() {
//        return priority;
//    }
//
//    public void setPriority(Integer priority) {
//        this.priority = priority;
//    }

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

//    public Set<CrisLayoutField> getLayoutFields() {
//        return layoutFields;
//    }
//
//    public void setLayoutFields(Set<CrisLayoutField> layoutFields) {
//        this.layoutFields = layoutFields;
//    }

//    public Set<CrisLayoutTab> getTabs() {
//        return tabs;
//    }
//
//    public void setTabs(Set<CrisLayoutTab> tabs) {
//        this.tabs = tabs;
//    }

    public Boolean getClear() {
        return clear;
    }

    public void setClear(Boolean clear) {
        this.clear = clear;
    }

//    public void addTab(CrisLayoutTab tab) {
//        if (this.tabs == null) {
//            this.tabs = new HashSet<>();
//        }
//        this.tabs.add(tab);
//    }
//
//    public void removeTab(CrisLayoutTab tab) {
//        if (this.tabs != null && !this.tabs.isEmpty()) {
//            this.tabs.remove(tab);
//        }
//    }

    public void addLayoutField(CrisLayoutField field, Integer position) {
        if (this.box2field.isEmpty()) {
            position = 0;
        } else if (position == null) {
            position = 0;
            for (Iterator<CrisLayoutBox2Field> it = this.box2field.iterator();
                    it.hasNext(); ) {
                CrisLayoutBox2Field b2f = it.next();
                if (b2f.getPosition() > position) {
                    position = b2f.getPosition();
                }
            }
        } else {
            int currentPosition = -1;
            for (Iterator<CrisLayoutBox2Field> it = this.box2field.iterator();
                    it.hasNext(); ) {
                CrisLayoutBox2Field b2f = it.next();
                currentPosition = b2f.getPosition();
                if (currentPosition >= position ) {
                    b2f.setPosition(++currentPosition);
                }
            }
            if (position > ++currentPosition) {
                position = currentPosition;
            }
        }
        CrisLayoutBox2Field box2field =
                new CrisLayoutBox2Field(this, field, position);
        this.box2field.add(box2field);
    }

    public void addLayoutField(CrisLayoutField field) {
        addLayoutField(field, null);
    }

    public void removeLayoutField(CrisLayoutField field) {
        boolean found = false;
        for (Iterator<CrisLayoutBox2Field> it = this.box2field.iterator();
                it.hasNext();) {
            CrisLayoutBox2Field b2f = it.next();
            if (found) {
                b2f.setPosition(b2f.getPosition() - 1);
            }
            if (b2f.getBox().equals(this) && b2f.getField().equals(field)) {
                it.remove();
                b2f.setBox(null);
                b2f.setField(null);
                found = true;
            }
        }
    }

    public List<CrisLayoutBox2Field> getBox2field() {
        return box2field;
    }

    public void setBox2field(List<CrisLayoutBox2Field> box2field) {
        this.box2field = box2field;
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
