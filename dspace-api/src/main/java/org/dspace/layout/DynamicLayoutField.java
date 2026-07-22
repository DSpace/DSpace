/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import static org.dspace.layout.DynamicLayoutFieldBitstream.BITSTREAM_FIELD_TYPE;
import static org.dspace.layout.DynamicLayoutFieldMetadata.METADATA_FIELD_TYPE;

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
@Table(name = "dynamic_layout_field")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class DynamicLayoutField implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_field_field_id_seq")
    @SequenceGenerator(
        name = "dynamic_layout_field_field_id_seq",
        sequenceName = "dynamic_layout_field_field_id_seq",
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
    private DynamicLayoutBox box;

    @Column(name = "style_label")
    private String styleLabel;

    @Column(name = "style_value")
    private String styleValue;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "dynamicLayoutField", cascade = CascadeType.ALL)
    @OrderBy(value = "priority")
    private List<DynamicMetadataGroup> dynamicMetadataGroupList = new ArrayList<>();

    @Column(name = "label_as_heading")
    private Boolean labelAsHeading;

    @Column(name = "values_inline")
    private Boolean valuesInline;

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
     * Returns the metadata field.
     */
    public MetadataField getMetadataField() {
        return metadataField;
    }

    /**
     * Sets the metadata field.
     */
    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    /**
     * Returns the rendering.
     */
    public String getRendering() {
        return rendering;
    }

    /**
     * Sets the rendering.
     */
    public void setRendering(String rendering) {
        this.rendering = rendering;
    }

    /**
     * Returns the row.
     */
    public Integer getRow() {
        return row;
    }

    /**
     * Sets the row.
     */
    public void setRow(Integer row) {
        this.row = row;
    }

    /**
     * Returns the priority.
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Returns the label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the row style.
     */
    public String getRowStyle() {
        return rowStyle;
    }

    /**
     * Sets the row style.
     */
    public void setRowStyle(String rowStyle) {
        this.rowStyle = rowStyle;
    }

    /**
     * Returns the cell style.
     */
    public String getCellStyle() {
        return cellStyle;
    }

    /**
     * Sets the cell style.
     */
    public void setCellStyle(String cellStyle) {
        this.cellStyle = cellStyle;
    }

    /**
     * Returns the box.
     */
    public DynamicLayoutBox getBox() {
        return box;
    }

    /**
     * Sets the box.
     */
    public void setBox(DynamicLayoutBox box) {
        this.box = box;
    }

    /**
     * Returns the style label.
     */
    public String getStyleLabel() {
        return styleLabel;
    }

    /**
     * Sets the style label.
     */
    public void setStyleLabel(String styleLabel) {
        this.styleLabel = styleLabel;
    }

    /**
     * Returns the style value.
     */
    public String getStyleValue() {
        return styleValue;
    }

    /**
     * Sets the style value.
     */
    public void setStyleValue(String styleValue) {
        this.styleValue = styleValue;
    }
    /**
     * Returns the dynamic metadata group list.
     */
    public List<DynamicMetadataGroup> getDynamicMetadataGroupList() {
        return dynamicMetadataGroupList;
    }

    /**
     * Adds a nested metadata group to this field.
     *
     * @param metadataGroup the metadata group to add
     */
    public void addDynamicMetadataGroupList(DynamicMetadataGroup metadataGroup) {
        this.dynamicMetadataGroupList.add(metadataGroup);
        metadataGroup.setDynamicLayoutField(this);
    }

    /**
     * Returns whether label as heading.
     */
    public Boolean isLabelAsHeading() {
        return labelAsHeading;
    }

    /**
     * Sets the label as heading.
     */
    public void setLabelAsHeading(Boolean labelAsHeading) {
        this.labelAsHeading = labelAsHeading;
    }

    /**
     * Returns whether values inline.
     */
    public Boolean isValuesInline() {
        return valuesInline;
    }

    /**
     * Sets the values inline.
     */
    public void setValuesInline(Boolean valuesInline) {
        this.valuesInline = valuesInline;
    }

    /**
     * Returns the cell.
     */
    public Integer getCell() {
        return cell;
    }

    /**
     * Sets the cell.
     */
    public void setCell(Integer cell) {
        this.cell = cell;
    }

    /**
     * Returns the type.
     */
    public String getType() {
        return METADATA_FIELD_TYPE;
    }

    /**
     * Returns whether metadata field.
     */
    public boolean isMetadataField() {
        return METADATA_FIELD_TYPE.equals(getType());
    }

    /**
     * Returns whether bitstream field.
     */
    public boolean isBitstreamField() {
        return BITSTREAM_FIELD_TYPE.equals(getType());
    }

}
