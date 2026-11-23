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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "dynamic_layout_box")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class DynamicLayoutBox implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_box_id_seq")
    @SequenceGenerator(name = "dynamic_layout_box_id_seq", sequenceName = "dynamic_layout_box_id_seq",
            allocationSize = 1)
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
        name = "dynamic_layout_box2securitymetadata",
        joinColumns = {@JoinColumn(name = "box_id")},
        inverseJoinColumns = {@JoinColumn(name = "metadata_field_id")}
    )
    private Set<MetadataField> metadataSecurityFields = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "box", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DynamicLayoutBox2SecurityGroup> box2SecurityGroups = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "box", cascade = CascadeType.ALL)
    @OrderBy(value = "row, cell, priority")
    private List<DynamicLayoutField> layoutFields = new ArrayList<>();

    @Column(name = "max_columns")
    private Integer maxColumns = null;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cell")
    private DynamicLayoutCell cell;

    @Column(name = "container")
    private Boolean container = true;

    @Override
    public Integer getID() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Returns the entitytype.
     */
    public EntityType getEntitytype() {
        return entitytype;
    }

    /**
     * Sets the entitytype.
     */
    public void setEntitytype(EntityType entitytype) {
        this.entitytype = entitytype;
    }

    /**
     * Returns the type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the collapsed.
     */
    public Boolean getCollapsed() {
        return collapsed;
    }

    /**
     * Sets the collapsed.
     */
    public void setCollapsed(Boolean collapsed) {
        this.collapsed = collapsed;
    }

    /**
     * Returns the shortname.
     */
    public String getShortname() {
        return shortname;
    }

    /**
     * Sets the shortname.
     */
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

    /**
     * Sets the security.
     */
    public void setSecurity(Integer security) {
        this.security = security;
    }

    /**
     * Returns whether public.
     */
    public boolean isPublic() {
        return getSecurity() == LayoutSecurity.PUBLIC.getValue();
    }

    /**
     * Returns whether not public.
     */
    public boolean isNotPublic() {
        return !isPublic();
    }


    /**
     * Returns the style.
     */
    public String getStyle() {
        return style;
    }

    /**
     * Sets the style.
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Returns the metadata security fields.
     */
    public Set<MetadataField> getMetadataSecurityFields() {
        return metadataSecurityFields;
    }

    /**
     * Sets the metadata security fields.
     */
    public void setMetadataSecurityFields(Set<MetadataField> metadataFields) {
        this.metadataSecurityFields = metadataFields;
    }

    /**
     * Adds the given metadata fields to the box metadata security configuration.
     *
     * @param metadataFields the metadata fields to add
     */
    public void addMetadataSecurityFields(Set<MetadataField> metadataFields) {
        this.metadataSecurityFields.addAll(metadataFields);
    }

    /**
     * Adds the given metadata field to the box metadata security configuration.
     *
     * @param metadataField the metadata field to add
     */
    public void addMetadataSecurityFields(MetadataField metadataField) {
        this.metadataSecurityFields.add(metadataField);
    }

    /**
     * Returns the layout fields.
     */
    public List<DynamicLayoutField> getLayoutFields() {
        return layoutFields;
    }

    /**
     * Adds a layout field to this box.
     *
     * @param layoutField the layout field to add
     */
    public void addLayoutField(DynamicLayoutField layoutField) {
        if (this.layoutFields == null) {
            this.layoutFields = new ArrayList<>();
        }
        this.layoutFields.add(layoutField);
        layoutField.setBox(this);
    }

    /**
     * Returns the max columns.
     */
    public Integer getMaxColumns() {
        return maxColumns;
    }

    /**
     * Sets the max columns.
     */
    public void setMaxColumns(Integer maxColumns) {
        this.maxColumns = maxColumns;
    }

    /**
     * Returns the cell.
     */
    public DynamicLayoutCell getCell() {
        return cell;
    }

    /**
     * Sets the cell.
     */
    public void setCell(DynamicLayoutCell cell) {
        this.cell = cell;
    }

    /**
     * Returns whether container.
     */
    public Boolean isContainer() {
        return container;
    }

    /**
     * Sets the container.
     */
    public void setContainer(Boolean container) {
        this.container = container;
    }

    /**
     * Returns the group security fields.
     */
    public Set<Group> getGroupSecurityFields() {
        return box2SecurityGroups.stream()
                                 .map(dynamicLayoutBox2SecurityGroup ->
                                     dynamicLayoutBox2SecurityGroup.getGroup())
                                 .collect(Collectors.toSet());
    }

    /**
     * Returns the box2 security groups.
     */
    public Set<DynamicLayoutBox2SecurityGroup> getBox2SecurityGroups() {
        return box2SecurityGroups;
    }

    /**
     * Sets the box2 security groups.
     */
    public void setBox2SecurityGroups(Set<DynamicLayoutBox2SecurityGroup> box2SecurityGroups) {
        this.box2SecurityGroups = box2SecurityGroups;
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
        DynamicLayoutBox other = (DynamicLayoutBox) obj;
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
