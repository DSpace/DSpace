/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

/**
 * This class is used to cary data between processes.
 * Using this class, we have a uniform, generalised single Object type containing the information used by different classes.
 * This Data Transfer Object contains all data for a call pertaining metadata, resulting in the possibility to return a larger quantity of information.
 * As this is a generalised class, we can use this across the external imports implementations
 *
 *  @author Philip Vissenaekens (philip at atmire dot com)
 *
 */
public class MetadatumDTO {

    private String schema;
    private String element;
    private String qualifier;
    private String value;

    /**
     * An empty MetadatumDTO constructor
     */
    public MetadatumDTO() {
    }

    /**
     * Retrieve the schema set to this MetadatumDTO.
     * Returns <tt>null</tt> of no schema is set
     * @return metadata field schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Set the schema to this MetadatumDTO
     * @param schema metadata field schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Retrieve the element set to this MetadatumDTO.
     * Returns <tt>null</tt> of no element is set
     * @return metadata field element
     */
    public String getElement() {
        return element;
    }

    /**
     * Set the element to this MetadatumDTO
     * @param element metadata field element
     */
    public void setElement(String element) {
        this.element = element;
    }

    /**
     * Retrieve the qualifier set to this MetadatumDTO.
     * Returns <tt>null</tt> of no qualifier is set
     * @return metadata field qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * Set the qualifier to this MetadatumDTO
     * @param qualifier metadata field qualifier
     */
    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Retrieve the value set to this MetadatumDTO.
     * Returns <tt>null</tt> of no value is set
     * @return metadata field value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the MetadatumDTO to this value.
     * @param value metadata field value
     */
    public void setValue(String value) {
        this.value = value;
    }
}
