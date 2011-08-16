/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;


import org.springframework.beans.factory.annotation.Required;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySortConfiguration {

    private String metadataField;
    private String type = DiscoveryConfigurationParameters.TYPE_TEXT;
    private boolean defaultSort = false;

    public String getMetadataField() {
        return metadataField;
    }

    @Required
    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefaultSort() {
        return defaultSort;
    }

    public void setDefaultSort(boolean defaultSort) {
        this.defaultSort = defaultSort;
    }
}
