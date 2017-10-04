/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.app.rest.DiscoveryRestController;
import org.dspace.app.rest.model.hateoas.SearchConfigurationResource;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by raf on 22/09/2017.
 */
public class FacetConfigurationRest extends BaseObjectRest<String> {

    public static final String NAME = "discover";
    public static final String CATEGORY = RestModel.DISCOVER;
    @JsonIgnore
    private String scope;
    @JsonIgnore
    private String configurationName;

    private LinkedList<SidebarFacet> sidebarFacets = new LinkedList<>();

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
    public void setScope(String scope){
        this.scope = scope;
    }

    public String getConfigurationName() {
        return configurationName;
    }
    public void setConfigurationName(String configurationName){
        this.configurationName = configurationName;
    }

    public List<SidebarFacet> getSidebarFacets(){ return sidebarFacets;}
    public void addSidebarFacet(SidebarFacet sidebarFacet){sidebarFacets.add(sidebarFacet);}

    @Override
    public boolean equals(Object object){
        return (object instanceof FacetConfigurationRest &&
                new EqualsBuilder().append(this.getCategory(), ((FacetConfigurationRest) object).getCategory())
                        .append(this.getType(), ((FacetConfigurationRest) object).getType())
                        .append(this.getController(), ((FacetConfigurationRest) object).getController())
                        .append(this.getScope(), ((FacetConfigurationRest) object).getScope())
                        .append(this.getConfigurationName(), ((FacetConfigurationRest) object).getConfigurationName())
                        .isEquals());
    }
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.getCategory())
                .append(this.getType())
                .append(this.getController())
                .append(this.getScope())
                .append(this.getConfigurationName())
                .toHashCode();
    }

    public static class SidebarFacet{
        private String name;
        private String type;
        public String getName(){ return name;}
        public void setName(String name){this.name=name;}
        public String getType(){ return type;}
        public void setType(String type){this.type=type;}
        @Override
        public boolean equals(Object object){
            return (object instanceof SidebarFacet &&
                    new EqualsBuilder().append(this.getName(), ((SidebarFacet) object).getName())
                            .isEquals());
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37)
                    .append(this.getName())
                    .toHashCode();
        }
    }


}
