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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dspace.app.rest.RestResourceController;
/**
 * The CrisLayoutMetadataComponent REST Resource
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class CrisLayoutMetadataComponentRest extends BaseObjectRest<String> {

    private static final long serialVersionUID = -4103165494268147700L;

    public static final String NAME = "metadatacomponent";
    public static final String CATEGORY = RestAddressableModel.LAYOUT;

    private String id;
    private List<Row> rows;

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestModel#getType()
     */
    @Override
    public String getType() {
        return NAME;
    }

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getCategory()
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    /* (non-Javadoc)
     * @see org.dspace.app.rest.model.RestAddressableModel#getController()
     */
    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    public static class Row {
        private List<Field> fields;
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
    }

    public static final class Field {
        @JsonInclude(Include.NON_NULL)
        private String metadata;
        @JsonInclude(Include.NON_NULL)
        private Bitstream bitstream;
        private String label;
        private String rendering;
        private String fieldType;
        private String style;
        public String getMetadata() {
            return metadata;
        }
        public void setMetadata(String metadata) {
            this.metadata = metadata;
        }
        public String getLabel() {
            return label;
        }
        public void setLabel(String label) {
            this.label = label;
        }
        public String getRendering() {
            return rendering;
        }
        public void setRendering(String rendering) {
            this.rendering = rendering;
        }
        public String getFieldType() {
            return fieldType;
        }
        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }
        public String getStyle() {
            return style;
        }
        public void setStyle(String style) {
            this.style = style;
        }
        public Bitstream getBitstream() {
            return bitstream;
        }
        public void setBitstream(Bitstream bitstream) {
            this.bitstream = bitstream;
        }
    }

    public static final class Bitstream {
        private String bundle;
        private Map<String, String> metadata;
        public String getBundle() {
            return bundle;
        }
        public void setBundle(String bundle) {
            this.bundle = bundle;
        }
        public Map<String, String> getMetadata() {
            return this.metadata;
        }
        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }
    }
}
