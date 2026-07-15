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
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.Group;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "dynamic_layout_tab")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@NamedEntityGraph(name = DynamicLayoutTab.ROWS_AND_CONTENT_GRAPH, attributeNodes = {
    @NamedAttributeNode(value = "rows", subgraph = "DynamicLayoutTab.cells_and_content"),
    @NamedAttributeNode(value = "entity")
    }, subgraphs = {
        @NamedSubgraph(name = "DynamicLayoutTab.cells_and_content", attributeNodes = {
            @NamedAttributeNode(value = "cells", subgraph = "DynamicLayoutTab.boxes_and_content")
        }),
        @NamedSubgraph(name = "DynamicLayoutTab.boxes_and_content", attributeNodes = {
            @NamedAttributeNode(value = "boxes")
        })
    }
)
public class DynamicLayoutTab implements ReloadableEntity<Integer> {

    public static final String ROWS_AND_CONTENT_GRAPH = "DynamicLayoutTab.rows_and_content";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_tab_seq")
    @SequenceGenerator(name = "dynamic_layout_tab_seq", sequenceName = "dynamic_layout_tab_id_seq", allocationSize = 1)
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

    @Column(name = "custom_filter")
    private String customFilter;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "dynamic_layout_tab2securitymetadata", joinColumns = {
        @JoinColumn(name = "tab_id") }, inverseJoinColumns = { @JoinColumn(name = "metadata_field_id") })
    private Set<MetadataField> metadataSecurityFields = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tab", cascade = CascadeType.ALL)
    private Set<DynamicLayoutTab2SecurityGroup> tab2SecurityGroups = new HashSet<>();

    @Column(name = "is_leading")
    private Boolean leading;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tab", cascade = CascadeType.ALL)
    @OrderColumn(name = "position")
    private List<DynamicLayoutRow> rows = new ArrayList<>();

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

    public void setSecurity(Integer security) {
        this.security = security;
    }

    public String getCustomFilter() {
        return customFilter;
    }

    public void setCustomFilter(String customFilter) {
        this.customFilter = customFilter;
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

    public Boolean isLeading() {
        return leading;
    }

    public void setLeading(Boolean leading) {
        this.leading = leading;
    }

    public void addRow(DynamicLayoutRow row) {
        getRows().add(row);
        row.setTab(this);
    }

    public List<DynamicLayoutRow> getRows() {
        return rows;
    }

    public List<DynamicLayoutBox> getBoxes() {
        return this.rows.stream()
            .flatMap(row -> row.getCells().stream())
            .flatMap(cell -> cell.getBoxes().stream())
            .collect(Collectors.toList());
    }

    public Set<Group> getGroupSecurityFields() {
        return tab2SecurityGroups.stream()
                                 .map(dynamicLayoutTab2SecurityGroup ->
                                     dynamicLayoutTab2SecurityGroup.getGroup())
                                 .collect(Collectors.toSet());
    }

    public Set<DynamicLayoutTab2SecurityGroup> getTab2SecurityGroups() {
        return tab2SecurityGroups;
    }

    public void setTab2SecurityGroups(Set<DynamicLayoutTab2SecurityGroup> tab2SecurityGroups) {
        this.tab2SecurityGroups = tab2SecurityGroups;
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
        DynamicLayoutTab other = (DynamicLayoutTab) obj;
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
