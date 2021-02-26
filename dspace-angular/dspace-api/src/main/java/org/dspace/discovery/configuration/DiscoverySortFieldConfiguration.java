/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySortFieldConfiguration {

    private String metadataField;
    private String type = DiscoveryConfigurationParameters.TYPE_TEXT;

    public String getMetadataField() {
        return metadataField;
    }

    @Autowired(required = true)
    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof DiscoverySortFieldConfiguration) {
            DiscoverySortFieldConfiguration compareConfig = (DiscoverySortFieldConfiguration) obj;
            if (compareConfig.getMetadataField().equals(getMetadataField()) && compareConfig.getType()
                                                                                            .equals(getType())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 19)
            .append(this.getMetadataField())
            .append(this.getType())
            .toHashCode();
    }
}
