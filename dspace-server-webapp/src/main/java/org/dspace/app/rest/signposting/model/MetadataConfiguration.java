/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

/**
 * Represents metadata handle configuration.
 */
public class MetadataConfiguration {

    private String metadataField;

    private String pattern;

    public MetadataConfiguration() {
    }

    public MetadataConfiguration(String metadataField, String pattern) {
        this.metadataField = metadataField;
        this.pattern = pattern;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
