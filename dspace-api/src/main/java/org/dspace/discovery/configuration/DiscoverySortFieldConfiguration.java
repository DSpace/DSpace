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
 * Created by IntelliJ IDEA.
 * User: Kevin
 * Date: 28/08/11
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */
public class DiscoverySortFieldConfiguration {

    private String metadataField;
    private String type = DiscoveryConfigurationParameters.TYPE_TEXT;

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

    @Override
    public boolean equals(Object obj) {
        if(obj != null && obj instanceof DiscoverySortFieldConfiguration){
            DiscoverySortFieldConfiguration compareConfig = (DiscoverySortFieldConfiguration) obj;
            if(compareConfig.getMetadataField().equals(getMetadataField()) && compareConfig.getType().equals(getType())){
                return true;
            }
        }
        return false;
    }

}
