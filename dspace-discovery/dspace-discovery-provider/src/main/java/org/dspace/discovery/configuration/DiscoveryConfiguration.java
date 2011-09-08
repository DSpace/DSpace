/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoveryConfiguration {

    /** The configuration for the sidebar facets **/
    private List<SidebarFacetConfiguration> sidebarFacets = new ArrayList<SidebarFacetConfiguration>();

    /** The default filter queries which will be applied to any search & the recent submissions **/
    private List<String> defaultFilterQueries;

    /** Configuration object for the recent submissions **/
    private DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration;

    /** The search filters which can be selected on the search page**/
    private List<DiscoverySearchFilter> searchFilters = new ArrayList<DiscoverySearchFilter>();

    private DiscoverySortConfiguration searchSortConfiguration;

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<SidebarFacetConfiguration> getSidebarFacets() {
        return sidebarFacets;
    }

    @Required
    public void setSidebarFacets(List<SidebarFacetConfiguration> sidebarFacets) {
        this.sidebarFacets = sidebarFacets;
    }

    public List<String> getDefaultFilterQueries() {
        //Since default filter queries are not mandatory we will return an empty list
        if(defaultFilterQueries == null){
            return new ArrayList<String>();
        }else{
            return defaultFilterQueries;
        }
    }

    public void setDefaultFilterQueries(List<String> defaultFilterQueries) {
        this.defaultFilterQueries = defaultFilterQueries;
    }

    public DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration() {
        return recentSubmissionConfiguration;
    }

    public void setRecentSubmissionConfiguration(DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration) {
        this.recentSubmissionConfiguration = recentSubmissionConfiguration;
    }

    public List<DiscoverySearchFilter> getSearchFilters() {
        return searchFilters;
    }

    @Required
    public void setSearchFilters(List<DiscoverySearchFilter> searchFilters) {
        this.searchFilters = searchFilters;
    }

    public DiscoverySortConfiguration getSearchSortConfiguration() {
        return searchSortConfiguration;
    }

    @Required
    public void setSearchSortConfiguration(DiscoverySortConfiguration searchSortConfiguration) {
        this.searchSortConfiguration = searchSortConfiguration;
    }
}
