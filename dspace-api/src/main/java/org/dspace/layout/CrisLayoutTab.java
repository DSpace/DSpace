/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.Set;
import javax.persistence.Cacheable;
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
        name = "cris_layout_tab2securityfield",
        joinColumns = {@JoinColumn(name = "tab_id")},
        inverseJoinColumns = {@JoinColumn(name = "authorized_field_id")}
    )
    private Set<MetadataField> metadataFields;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cris_layout_tab2box",
        joinColumns = {@JoinColumn(name = "cris_layout_tab_id")},
        inverseJoinColumns = {@JoinColumn(name = "cris_layout_box_id")}
    )
    private Set<CrisLayoutBox> boxes;

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

    public Set<MetadataField> getMetadataFields() {
        return metadataFields;
    }

    public void setMetadataFields(Set<MetadataField> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public Set<CrisLayoutBox> getBoxes() {
        return boxes;
    }

    public void setBoxes(Set<CrisLayoutBox> boxes) {
        this.boxes = boxes;
    }

}
