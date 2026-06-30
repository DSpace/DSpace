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
 * The CrisLayoutMetadataConfiguration details
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutMetadataConfigurationRest implements CrisLayoutBoxConfigurationRest {

    public static final String NAME = "boxmetadataconfiguration";

    private List<Row> rows = new ArrayList<>();

    private String type = NAME;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

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

        public List<Cell> getCells() {
            return cells;
        }

        public void setCells(List<Cell> cells) {
            this.cells = cells;
        }

        public void addCell(Cell cell) {
            if (this.cells == null) {
                this.cells = new ArrayList<>();
            }
            this.cells.add(cell);
        }

        public String getStyle() {
            return style;
        }

        public void setStyle(String style) {
            this.style = style;
        }
    }

    public static class Cell {

        @JsonInclude(Include.NON_NULL)
        private String style;

        private List<Field> fields = new ArrayList<>();

        public List<Field> getFields() {
            return fields;
        }

        public void setFields(List<Field> fields) {
            this.fields = fields;
        }

        public void addField(Field field) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(field);
        }

        public String getStyle() {
            return style;
        }

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

        public String getMetadata() {
            return metadata;
        }

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

        public Bitstream getBitstream() {
            return bitstream;
        }

        public void setBitstream(Bitstream bitstream) {
            this.bitstream = bitstream;
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

        public void setMetadataGroup(MetadataGroup metadataGroup) {
            this.metadataGroup = metadataGroup;
        }

        public MetadataGroup getMetadataGroup() {
            return this.metadataGroup;
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
    }

    public static final class Bitstream {

        private String bundle;

        private String metadataField;

        private String metadataValue;

        public String getBundle() {
            return bundle;
        }

        public void setBundle(String bundle) {
            this.bundle = bundle;
        }

        public String getMetadataField() {
            return metadataField;
        }

        public void setMetadataField(String metadataField) {
            this.metadataField = metadataField;
        }

        public String getMetadataValue() {
            return metadataValue;
        }

        public void setMetadataValue(String metadataValue) {
            this.metadataValue = metadataValue;
        }
    }

    public static final class MetadataGroup {

        private String leading;

        private List<Field> elements = new ArrayList<>();

        public String getLeading() {
            return leading;
        }

        public List<Field> getElements() {
            return elements;
        }

        public void setElements(List<Field> elements) {
            this.elements = elements;
        }

        public void setLeading(String leading) {
            this.leading = leading;
        }

    }
}
