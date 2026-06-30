/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Entity(name = "CrisLayoutFieldBitstream")
@DiscriminatorValue(CrisLayoutFieldBitstream.BITSTREAM_FIELD_TYPE)
public class CrisLayoutFieldBitstream extends CrisLayoutField {

    public static final String BITSTREAM_FIELD_TYPE = "BITSTREAM";

    @Column(name = "bundle")
    private String bundle;
    @Column(name = "metadata_value")
    private String metadataValue;

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }

    @Override
    public String getType() {
        return BITSTREAM_FIELD_TYPE;
    }
}
