/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The DynamicLayoutMetadataConfiguration details
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class DynamicLayoutMetadataConfigurationRest implements DynamicLayoutBoxConfigurationRest {

    public static final String NAME = "boxmetadataconfiguration";

    private List<Row> rows = new ArrayList<>();

    private String type = NAME;

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
     * Returns the rows.
     */
    public List<Row> getRows() {
        return rows;
    }

    /**
     * Sets the rows.
     */
    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    /**
     * Adds a row to this configuration.
     *
     * @param row the row to add
     */
    public void addRow(Row row) {
        if (this.rows == null) {
            this.rows = new ArrayList<>();
        }
        this.rows.add(row);
    }

    public static class Row {

        @JsonInclude(Include.NON_NULL)
        private String style;

        private List<Cell> cells = new ArrayList<>();

        /**
         * Returns the cells.
         */
        public List<Cell> getCells() {
            return cells;
        }

        /**
         * Sets the cells.
         */
        public void setCells(List<Cell> cells) {
            this.cells = cells;
        }

        /**
         * Adds a cell to this row.
         *
         * @param cell the cell to add
         */
        public void addCell(Cell cell) {
            if (this.cells == null) {
                this.cells = new ArrayList<>();
            }
            this.cells.add(cell);
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
    }

    public static class Cell {

        @JsonInclude(Include.NON_NULL)
        private String style;

        private List<Field> fields = new ArrayList<>();

        /**
         * Returns the fields.
         */
        public List<Field> getFields() {
            return fields;
        }

        /**
         * Sets the fields.
         */
        public void setFields(List<Field> fields) {
            this.fields = fields;
        }

        /**
         * Adds a field to this cell.
         *
         * @param field the field to add
         */
        public void addField(Field field) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(field);
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
    }


    public static final class Field {

        @JsonInclude(Include.NON_NULL)
        private String metadata;

        @JsonInclude(Include.NON_NULL)
        private Bitstream bitstream;

        private String label;

        private String rendering;

        private String fieldType;

        private String styleLabel;

        private String styleValue;

        @JsonInclude(Include.NON_NULL)
        private MetadataGroup metadataGroup;

        @JsonInclude(Include.NON_NULL)
        private Boolean labelAsHeading;

        @JsonInclude(Include.NON_NULL)
        private Boolean valuesInline;

        /**
         * Returns the metadata.
         */
        public String getMetadata() {
            return metadata;
        }

        /**
         * Sets the metadata.
         */
        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }

        /**
         * This attribute is the i18n key for the field label to visualize
         * @return
         */
        public String getLabel() {
            return label;
        }

        /**
         * This attribute is the i18n key for the field label to visualize
         * @param label
         */
        public void setLabel(String label) {
            this.label = label;
        }

        /**
         * This attribute defines the component to use to visualize the field. Examples
         * are browselink, longtext, identifier, date, etc. for metadata field and
         * preview, thubmnail, etc. for bitstream field
         * @return
         */
        public String getRendering() {
            return rendering;
        }

        /**
         * This attribute defines the component to use to visualize the field. Examples
         * are browselink, longtext, identifier, date, etc. for metadata field and
         * preview, thubmnail, etc. for bitstream field
         * @param rendering
         */
        public void setRendering(String rendering) {
            this.rendering = rendering;
        }

        /**
         * This is one of metadata or bitstream a corresponding attribute will be
         * present
         * <ul>
         * <li>metadata: is the canonical name of the metadata to use (eg
         * dc.contributor.author, dc.title, etc.)</li>
         * <li>bitstream: is an object containing details to filter the bitstreams to
         * use. It can be the name of the bundle to use and/or the value of specfic
         * bitstream metadata</li>
         * </ul>
         * @return
         */
        public String getFieldType() {
            return fieldType;
        }

        /**
         * This is one of metadata or bitstream a corresponding attribute will be
         * present
         * <ul>
         * <li>metadata: is the canonical name of the metadata to use (eg
         * dc.contributor.author, dc.title, etc.)</li>
         * <li>bitstream: is an object containing details to filter the bitstreams to
         * use. It can be the name of the bundle to use and/or the value of specfic
         * bitstream metadata</li>
         * </ul>
         * @return
         */
        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        /**
         * Returns the bitstream.
         */
        public Bitstream getBitstream() {
            return bitstream;
        }

        /**
         * Sets the bitstream.
         */
        public void setBitstream(Bitstream bitstream) {
            this.bitstream = bitstream;
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
         * Sets the metadata group.
         */
        public void setMetadataGroup(MetadataGroup metadataGroup) {
            this.metadataGroup = metadataGroup;
        }

        /**
         * Returns the metadata group.
         */
        public MetadataGroup getMetadataGroup() {
            return this.metadataGroup;
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
    }

    public static final class Bitstream {

        private String bundle;

        private String metadataField;

        private String metadataValue;

        /**
         * Returns the bundle.
         */
        public String getBundle() {
            return bundle;
        }

        /**
         * Sets the bundle.
         */
        public void setBundle(String bundle) {
            this.bundle = bundle;
        }

        /**
         * Returns the metadata field.
         */
        public String getMetadataField() {
            return metadataField;
        }

        /**
         * Sets the metadata field.
         */
        public void setMetadataField(String metadataField) {
            this.metadataField = metadataField;
        }

        /**
         * Returns the metadata value.
         */
        public String getMetadataValue() {
            return metadataValue;
        }

        /**
         * Sets the metadata value.
         */
        public void setMetadataValue(String metadataValue) {
            this.metadataValue = metadataValue;
        }
    }

    public static final class MetadataGroup {

        private String leading;

        private List<Field> elements = new ArrayList<>();

        /**
         * Returns the leading.
         */
        public String getLeading() {
            return leading;
        }

        /**
         * Returns the elements.
         */
        public List<Field> getElements() {
            return elements;
        }

        /**
         * Sets the elements.
         */
        public void setElements(List<Field> elements) {
            this.elements = elements;
        }

        /**
         * Sets the leading.
         */
        public void setLeading(String leading) {
            this.leading = leading;
        }

    }
}
