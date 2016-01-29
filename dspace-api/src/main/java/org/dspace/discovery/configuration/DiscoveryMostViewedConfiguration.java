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
 * @author l.pascarelli
 *
 */
public class DiscoveryMostViewedConfiguration {

    private String type;

    private int max = 5;
    private boolean useAsHomePage;

    private DiscoveryViewConfiguration metadataFields; 
    

    public int getMax() {
        return max;
    }

    @Required
    public void setMax(int max) {
        this.max = max;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUseAsHomePage(boolean useAsHomePage) {
        this.useAsHomePage = useAsHomePage;
    }

    public boolean getUseAsHomePage() {
        return useAsHomePage;
    }

    public DiscoveryViewConfiguration getMetadataFields()
    {
        return metadataFields;
    }

    public void setMetadataFields(DiscoveryViewConfiguration metadataFields)
    {
        this.metadataFields = metadataFields;
    }
}
