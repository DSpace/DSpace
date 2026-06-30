/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import static org.dspace.layout.CrisLayoutFieldBitstream.BITSTREAM_FIELD_TYPE;
import static org.dspace.layout.CrisLayoutFieldMetadata.METADATA_FIELD_TYPE;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;


@Entity
@Table(name = "cris_layout_field")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutField implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_field_field_id_seq")
    @SequenceGenerator(
        name = "cris_layout_field_field_id_seq",
        sequenceName = "cris_layout_field_field_id_seq",
        allocationSize = 1)
    @Column(name = "field_id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField;

    @Column(name = "rendering")
    private String rendering;

    @Column(name = "row", nullable = false)
    private Integer row;

    @Column(name = "cell", nullable = false)
    private Integer cell;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "label")
    private String label;

    @Column(name = "row_style")
    private String rowStyle;

    @Column(name = "cell_style")
    private String cellStyle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private CrisLayoutBox box;

    @Column(name = "style_label")
    private String styleLabel;

    @Column(name = "style_value")
    private String styleValue;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "crisLayoutField", cascade = CascadeType.ALL)
    @OrderBy(value = "priority")
    private List<CrisMetadataGroup> crisMetadataGroupList = new ArrayList<>();

    @Column(name = "label_as_heading")
    private Boolean labelAsHeading;

    @Column(name = "values_inline")
    private Boolean valuesInline;

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    public String getRendering() {
        return rendering;
    }

    public void setRendering(String rendering) {
        this.rendering = rendering;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRowStyle() {
        return rowStyle;
    }

    public void setRowStyle(String rowStyle) {
        this.rowStyle = rowStyle;
    }

    public String getCellStyle() {
        return cellStyle;
    }

    public void setCellStyle(String cellStyle) {
        this.cellStyle = cellStyle;
    }

    public CrisLayoutBox getBox() {
        return box;
    }

    public void setBox(CrisLayoutBox box) {
        this.box = box;
    }

    public String getStyleLabel() {
        return styleLabel;
    }

    public void setStyleLabel(String styleLabel) {
        this.styleLabel = styleLabel;
    }

    public String getStyleValue() {
        return styleValue;
    }

    public void setStyleValue(String styleValue) {
        this.styleValue = styleValue;
    }
    public List<CrisMetadataGroup> getCrisMetadataGroupList() {
        return crisMetadataGroupList;
    }

    public void addCrisMetadataGroupList(CrisMetadataGroup metadataGroup) {
        this.crisMetadataGroupList.add(metadataGroup);
        metadataGroup.setCrisLayoutField(this);
    }

    public Boolean isLabelAsHeading() {
        return labelAsHeading;
    }

    public void setLabelAsHeading(Boolean labelAsHeading) {
        this.labelAsHeading = labelAsHeading;
    }

    public Boolean isValuesInline() {
        return valuesInline;
    }

    public void setValuesInline(Boolean valuesInline) {
        this.valuesInline = valuesInline;
    }

    public Integer getCell() {
        return cell;
    }

    public void setCell(Integer cell) {
        this.cell = cell;
    }

    public String getType() {
        return METADATA_FIELD_TYPE;
    }

    public boolean isMetadataField() {
        return METADATA_FIELD_TYPE.equals(getType());
    }

    public boolean isBitstreamField() {
        return BITSTREAM_FIELD_TYPE.equals(getType());
    }

}
