/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

public class MetadataValueVO {

    private final String value;

    private final String authority;

    private final int confidence;

    public MetadataValueVO(String value, String authority, int confidence) {
        super();
        this.value = value;
        this.authority = authority;
        this.confidence = confidence;
    }

    public String getValue() {
        return value;
    }

    public String getAuthority() {
        return authority;
    }

    public int getConfidence() {
        return confidence;
    }

}
