/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 19/09/12
 * Time: 10:11
 */
public class MetadataFieldConfig {
    private String schema;
    private String element;
    private String qualifier;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MetadataFieldConfig that = (MetadataFieldConfig) o;

        if (!element.equals(that.element)) return false;
        if (qualifier != null ? !qualifier.equals(that.qualifier) : that.qualifier != null) return false;
        if (!schema.equals(that.schema)) return false;

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MetadataField");
        sb.append("{schema='").append(schema).append('\'');
        sb.append(", element='").append(element).append('\'');
        sb.append(", qualifier='").append(qualifier).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = schema.hashCode();
        result = 31 * result + element.hashCode();
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }

    public String getSchema() {

        return schema;
    }

    public MetadataFieldConfig(MetadatumDTO value) {
        this.schema = value.getSchema();
        this.element = value.getElement();
        this.qualifier = value.getQualifier();
    }

    public MetadataFieldConfig() {
    }

    public MetadataFieldConfig(String schema, String element, String qualifier) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
    }

    public MetadataFieldConfig(String full) {
        String elements[]=full.split("\\.");
        if(elements.length==2){
            this.schema = elements[0];
            this.element =elements[1];
        } else if(elements.length==3){
            this.schema = elements[0];
            this.element =elements[1];
            this.qualifier = elements[2];
        }

    }

    public MetadataFieldConfig(String schema, String element) {
        this.schema = schema;
        this.element = element;
        this.qualifier = null;
    }

    public void setSchema(String schema) {
        this.schema = schema;

    }

    public String getField() {
        return schema + "." + element + (qualifier==null?"":("." + qualifier));
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
}
