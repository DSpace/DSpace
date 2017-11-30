/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.springframework.beans.factory.annotation.Required;

import java.util.List;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySearchFilter {

    protected String indexFieldName;
    protected List<String> metadataFields;
    protected String type = DiscoveryConfigurationParameters.TYPE_TEXT;
    public static final String FILTER_TYPE_DEFAULT = "default";

    public String getIndexFieldName() {
        return indexFieldName;
    }

    @Required
    public void setIndexFieldName(String indexFieldName) {
        this.indexFieldName = indexFieldName;
    }

    public List<String> getMetadataFields() {
        return metadataFields;
    }

    @Required
    public void setMetadataFields(List<String> metadataFields) {
        this.metadataFields = metadataFields;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) throws DiscoveryConfigurationException {
        if(type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_TEXT))
        {
            this.type = DiscoveryConfigurationParameters.TYPE_TEXT;
        } else
        if(type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_DATE))
        {
            this.type = DiscoveryConfigurationParameters.TYPE_DATE;
        } else
        if(type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL))
        {
            throw new DiscoveryConfigurationException("The " + type + " can't be used with a default side bar facet use the \"HierarchicalSidebarFacetConfiguration\" class instead.");
        }else{
            this.type = type;
        }
    }

    public String getFilterType(){
        return FILTER_TYPE_DEFAULT;
    }

}
