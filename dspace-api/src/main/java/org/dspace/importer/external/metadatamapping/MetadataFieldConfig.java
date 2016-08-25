/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

/**
 * A generalised configuration for metadatafields.
 * This is used to make the link between values and the actual MetadatumDTO object.
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class MetadataFieldConfig {
    private String schema;
    private String element;
    private String qualifier;


    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o the reference object with which to compare.
     * @return  {@code true} if this object is the same as the obj
     *          argument; {@code false} otherwise.
     */
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

    /**
     * Create the String representation of the MetadataFieldConfig
     * @return a string representation of the MetadataFieldConfig
     */
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

    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     * @return  a hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = schema.hashCode();
        result = 31 * result + element.hashCode();
        result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
        return result;
    }


    /**
     * Create a MetadataFieldConfig based on a given MetadatumDTO
     * This MetadatumDTO object contains the schema, element and qualifier needed to initialize the MetadataFieldConfig
     * @param value
     */
    public MetadataFieldConfig(MetadatumDTO value) {
        this.schema = value.getSchema();
        this.element = value.getElement();
        this.qualifier = value.getQualifier();
    }

    /**
     * An empty initialization of MetadataFieldConfig
     */
    public MetadataFieldConfig() {
    }

    /**
     * Create a MetadataFieldConfig using a schema,element and qualifier
     * @param schema The schema to set to this object
     * @param element The element to set to this object
     * @param qualifier The qualifier to set to this object
     */
    public MetadataFieldConfig(String schema, String element, String qualifier) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
    }

    /**
     * Create a MetadataFieldConfig using a single value.
     * This value is split up into schema, element and qualifier, based on a dot(.)
     * @param full A string representing the schema.element.qualifier triplet
     */
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
    /**
     * Create a MetadataFieldConfig using a schema and element
     * qualifier will be set to <code>null</code>
     * @param schema The schema to set to this object
     * @param element The element to set to this object
     */
    public MetadataFieldConfig(String schema, String element) {
        this.schema = schema;
        this.element = element;
        this.qualifier = null;
    }

    /**
     * Set the schema to this MetadataFieldConfig
     * @param schema The schema to set to this object
     */
    public void setSchema(String schema) {
        this.schema = schema;

    }

    /**
     * Return the schema set to this object.
     * <code>null</code> if nothing is set
     * @return The schema of this object
     */
    public String getSchema() {

        return schema;
    }

    /**
     * Return a string representing the field of this object
     * @return The field that is set to this object, in the form of schema.element.qualifier
     */
    public String getField() {
        return schema + "." + element + (qualifier==null?"":("." + qualifier));
    }

    /**
     * Return the qualifier set to this object.
     * <code>null</code> if nothing is set
     * @return The qualifier of this object
     */
    public String getElement() {
        return element;
    }

    /**
     * Set the element to this MetadataFieldConfig
     * @param element The element to set to this object
     */
    public void setElement(String element) {
        this.element = element;
    }

    /**
     * Return the qualifier set to this object.
     * <code>null</code> if nothing is set
     * @return The qualifier of this object
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Set the qualifier to this MetadataFieldConfig
     * @param qualifier The qualifier to set to this object
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
}
