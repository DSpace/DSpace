/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;

/**
 * This class' purpose is to give information about the FacetConfiguration to be displayed on the /facets endpoint
 */
public class FacetConfigurationRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;

    private String scope;

    private String configuration;

    @JsonIgnore
    private LinkedList<SearchFacetEntryRest> sidebarFacets = new LinkedList<>();

    public String getCategory() {
        return CATEGORY;
    }

    public String getType() {
        return NAME;
    }

    public Class getController() {
        return DiscoveryRestController.class;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configurationName) {
        this.configuration = configurationName;
    }

    public List<SearchFacetEntryRest> getSidebarFacets() {
        return sidebarFacets;
    }

    public void addSidebarFacet(SearchFacetEntryRest sidebarFacet) {
        sidebarFacets.add(sidebarFacet);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof FacetConfigurationRest &&
            new EqualsBuilder().append(this.getCategory(), ((FacetConfigurationRest) object).getCategory())
                               .append(this.getType(), ((FacetConfigurationRest) object).getType())
                               .append(this.getController(), ((FacetConfigurationRest) object).getController())
                               .append(this.getScope(), ((FacetConfigurationRest) object).getScope())
                               .append(this.getConfiguration(),
                                       ((FacetConfigurationRest) object).getConfiguration())
                               .append(this.getSidebarFacets(), ((FacetConfigurationRest) object).getSidebarFacets())
                               .isEquals());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(this.getCategory())
            .append(this.getType())
            .append(this.getController())
            .append(this.getScope())
            .append(this.getConfiguration())
            .append(this.getSidebarFacets())
            .toHashCode();
    }

}
