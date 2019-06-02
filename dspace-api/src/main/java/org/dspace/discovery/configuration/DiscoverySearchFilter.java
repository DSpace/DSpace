/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.List;

import org.springframework.beans.factory.annotation.Required;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySearchFilter {

    protected String indexFieldName;
    protected List<String> metadataFields;
    protected String type = DiscoveryConfigurationParameters.TYPE_TEXT;
    public static final String FILTER_TYPE_DEFAULT = "default";
    protected boolean isOpenByDefault = false;

    protected int pageSize;

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

    /**
      * Returns the type of the DiscoverySearchFilter
      * @return  The type of the DiscoverySearchFilter
      */
    public String getType() {
        return type;
    }

    /**
      * Sets the type of the DiscoverySearchFilter to the one given in the parameter if it matches
      * a set of possible types
      * The possible types are described in: {@link org.dspace.discovery.configuration.DiscoveryConfigurationParameters}
      * For the DiscoverySearchFilter only the TYPE_TEXT, TYPE_DATE and TYPE_HIERARCHICAL are allowed
      *
      * @param type  The type for this DiscoverySearchFilter
      * @throws DiscoveryConfigurationException  If none of the types match, this error will be thrown indiciating this
      */
    public void setType(String type) throws DiscoveryConfigurationException {
        if (type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_TEXT)) {
            this.type = DiscoveryConfigurationParameters.TYPE_TEXT;
        } else if (type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_DATE)) {
            this.type = DiscoveryConfigurationParameters.TYPE_DATE;
        } else if (type.equalsIgnoreCase(DiscoveryConfigurationParameters.TYPE_HIERARCHICAL)) {
            throw new DiscoveryConfigurationException(
                "The " + type + " can't be used with a default side bar facet use the " +
                    "\"HierarchicalSidebarFacetConfiguration\" class instead.");
        } else {
            this.type = type;
        }
    }

    public String getFilterType() {
        return FILTER_TYPE_DEFAULT;
    }

    /**
     * This method returns a boolean value indicating whether the search filter
     * should be open or closed by default in the UI
     * @return  A boolean value indicating whether the search filter in the ui should be open
     *          or closed by default
     */
    public boolean isOpenByDefault() {
        return isOpenByDefault;
    }

    /**
     * Sets the DiscoverySearchFilter to be open by default or not depending on the parameter given
     * @param isOpenByDefault A boolean value that will indicate whether this DiscoverySearchFilter
     *                        should be open by default or not in the UI.
     */
    public void setIsOpenByDefault(boolean isOpenByDefault) {
        this.isOpenByDefault = isOpenByDefault;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
