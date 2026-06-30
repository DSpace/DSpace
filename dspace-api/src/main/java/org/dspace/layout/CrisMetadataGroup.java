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
@Table(name = "cris_layout_field2nested")
public class CrisMetadataGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_field_nested_id_seq")
    @SequenceGenerator(
            name = "cris_layout_field_nested_id_seq",
            sequenceName = "cris_layout_field_nested_id_seq",
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
    private CrisLayoutField crisLayoutField;

    public Integer getId() {
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
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

    public CrisLayoutField getCrisLayoutField() {
        return crisLayoutField;
    }

    public void setCrisLayoutField(CrisLayoutField crisLayoutField) {
        this.crisLayoutField = crisLayoutField;
    }

}