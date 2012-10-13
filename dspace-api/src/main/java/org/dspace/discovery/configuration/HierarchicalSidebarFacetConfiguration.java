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
 * Special sidebar facet configuration used for hierarchical facets
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class HierarchicalSidebarFacetConfiguration extends DiscoverySearchFilterFacet{

    private String splitter;
    private boolean skipFirstNodeLevel = true;


    public HierarchicalSidebarFacetConfiguration() {
        //Our default type is the hieracrhical, can be overridden by the configuration
        this.type = DiscoveryConfigurationParameters.TYPE_HIERARCHICAL;
    }

    public String getSplitter() {
        return splitter;
    }

    @Required
    public void setSplitter(String splitter) {
        this.splitter = splitter;
    }

    public boolean isSkipFirstNodeLevel() {
        return skipFirstNodeLevel;
    }

    public void setSkipFirstNodeLevel(boolean skipFirstNodeLevel) {
        this.skipFirstNodeLevel = skipFirstNodeLevel;
    }

    @Override
    public void setType(String type) throws DiscoveryConfigurationException {
        if(type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)){
            this.type = type;
        }else{
            throw new DiscoveryConfigurationException("The " + type + " can't be used with a hierarchical facet side bar facet use the \"DiscoverySearchFilterFacet\" class instead.");
        }

    }
}
