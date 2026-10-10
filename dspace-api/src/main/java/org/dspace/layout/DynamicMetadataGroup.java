/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.MetadataField;

@Entity
@Table(name = "dynamic_layout_field2nested")
public class DynamicMetadataGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dynamic_layout_field_nested_id_seq")
    @SequenceGenerator(
            name = "dynamic_layout_field_nested_id_seq",
            sequenceName = "dynamic_layout_field_nested_id_seq",
            allocationSize = 1)
    @Column(name = "nested_field_id", unique = true, nullable = false, insertable = true)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField;

    @Column(name = "rendering")
    private String rendering;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "label")
    private String label;

    @Column(name = "style")
    private String style;

    @Column(name = "style_label")
    private String styleLabel;

    @Column(name = "style_value")
    private String styleValue;

    @ManyToOne
    @JoinColumn(name = "field_id")
    private DynamicLayoutField dynamicLayoutField;

    /**
     * Returns the id.
     */
    public Integer getId() {
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
     * Returns the dynamic layout field.
     */
    public DynamicLayoutField getDynamicLayoutField() {
        return dynamicLayoutField;
    }

    /**
     * Sets the dynamic layout field.
     */
    public void setDynamicLayoutField(DynamicLayoutField dynamicLayoutField) {
        this.dynamicLayoutField = dynamicLayoutField;
    }

}