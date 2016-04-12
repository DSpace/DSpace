/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping;

/**
 * @author Philip Vissenaekens (philip at atmire dot com)
 * Date: 21/10/15
 * Time: 09:52
 *
 * This class is used to cary data between processes.
 * Using this class, we have a uniform, generalised single Object type containing the information used by different classes.
 * This Data Transfer Object contains all data for a call pertaining metadata, resulting in the possibility to return a larger quantity of information.
 * As this is a generalised class, we can use this across the external imports implementations
 */
public class MetadatumDTO {

    private String schema;
    private String element;
    private String qualifier;
    private String value;

    public MetadatumDTO() {
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
