/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.vo;

import org.dspace.content.MetadataValue;

/**
 * A value object that contains a metadata value, authority and confidence.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataValueVO {

    private final String value;

    private final String authority;

    private final int confidence;

    private final Integer securityLevel;

    public MetadataValueVO(String value) {
        this(value, null, -1, null);
    }

    public MetadataValueVO(String value, String authority) {
        this(value, authority, 600, null);
    }

    public MetadataValueVO(String value, String authority, int confidence, Integer securityLevel) {
        this.value = value;
        this.authority = authority;
        this.confidence = confidence;
        this.securityLevel = securityLevel;
    }

    public MetadataValueVO(MetadataValue metadataValue) {
        this(metadataValue.getValue(), metadataValue.getAuthority(), metadataValue.getConfidence(),
            metadataValue.getSecurityLevel());
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

    public Integer getSecurityLevel() {
        return securityLevel;
    }

}
