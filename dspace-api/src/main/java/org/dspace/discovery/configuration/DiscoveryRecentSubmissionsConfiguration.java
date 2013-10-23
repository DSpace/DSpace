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
public class DiscoveryRecentSubmissionsConfiguration {

    private String metadataSortField;
    private String type;

    private int max = 5;
    private boolean useAsHomePage;

    public String getMetadataSortField() {
        return metadataSortField;
    }

    @Required
    public void setMetadataSortField(String metadataSortField) {
        this.metadataSortField = metadataSortField;
    }

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
}
