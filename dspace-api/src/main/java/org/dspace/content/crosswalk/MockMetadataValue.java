/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;

/**
 * Metadata Value is bound to a database, the dissemination crosswalk require mock metadata just need for desimanation
 * This class provides a wrapper for this.
 * This class should only be used for the dissemniation metadata values that aren't to be written to the database
 *
 * @author kevinvandevelde at atmire.com
 */
public class MockMetadataValue {

    private String schema;
    private String element;
    private String qualifier;
    private String language;
    private String value;
    private String authority;
    private int confidence;

    public MockMetadataValue(MetadataValue metadataValue)
    {
        MetadataField metadataField = metadataValue.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        schema = metadataSchema.getName();
        element = metadataField.getElement();
        qualifier = metadataField.getQualifier();
        language = metadataValue.getLanguage();
        value = metadataValue.getValue();
        authority = metadataValue.getAuthority();
        confidence = metadataValue.getConfidence();
    }

    public MockMetadataValue() {
    }

    public String getSchema() {
        return schema;
    }

    public String getElement() {
        return element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getValue() {
        return value;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
}
