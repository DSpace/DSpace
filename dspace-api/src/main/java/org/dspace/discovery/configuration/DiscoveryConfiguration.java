/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoveryConfiguration implements InitializingBean {

    /**
     * The configuration for the sidebar facets
     **/
    private List<DiscoverySearchFilterFacet> sidebarFacets = new ArrayList<>();

    private TagCloudFacetConfiguration tagCloudFacetConfiguration = new TagCloudFacetConfiguration();

    /**
     * The default filter queries which will be applied to any search & the recent submissions
     **/
    private List<String> defaultFilterQueries;

    /**
     * Configuration object for the recent submissions
     **/
    private DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration;

    /**
     * The search filters which can be selected on the search page
     **/
    private List<DiscoverySearchFilter> searchFilters = new ArrayList<>();

    private DiscoverySortConfiguration searchSortConfiguration;

    private int defaultRpp = 10;

    private String id;
    private DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration;
    private DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration;
    private boolean spellCheckEnabled;
    private boolean indexAlways = false;

    /**
     * The `indexAlways` property determines whether the configuration should always be included when indexing items.
     * The default value is false which implies the configuration is only used when it matches the collection or if
     * it's the default configuration
     * When set to true, the configuration is also used to index an item without a specific collection mapping
     * This can be used for displaying different facets depending on the type of item instead of the collection
     */
    public boolean isIndexAlways() {
        return indexAlways;
    }

    public void setIndexAlways(boolean indexAlways) {
        this.indexAlways = indexAlways;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<DiscoverySearchFilterFacet> getSidebarFacets() {
        return sidebarFacets;
    }

    @Required
    public void setSidebarFacets(List<DiscoverySearchFilterFacet> sidebarFacets) {
        this.sidebarFacets = sidebarFacets;
    }

    public TagCloudFacetConfiguration getTagCloudFacetConfiguration() {
        return tagCloudFacetConfiguration;
    }

    public void setTagCloudFacetConfiguration(TagCloudFacetConfiguration tagCloudFacetConfiguration) {
        this.tagCloudFacetConfiguration = tagCloudFacetConfiguration;
    }

    public List<String> getDefaultFilterQueries() {
        //Since default filter queries are not mandatory we will return an empty list
        if (defaultFilterQueries == null) {
            return new ArrayList<String>();
        } else {
            return defaultFilterQueries;
        }
    }

    public void setDefaultFilterQueries(List<String> defaultFilterQueries) {
        this.defaultFilterQueries = defaultFilterQueries;
    }

    public DiscoveryRecentSubmissionsConfiguration getRecentSubmissionConfiguration() {
        return recentSubmissionConfiguration;
    }

    public void setRecentSubmissionConfiguration(
        DiscoveryRecentSubmissionsConfiguration recentSubmissionConfiguration) {
        this.recentSubmissionConfiguration = recentSubmissionConfiguration;
    }

    public List<DiscoverySearchFilter> getSearchFilters() {
        return searchFilters;
    }

    public DiscoverySearchFilter getSearchFilter(String name) {
        for (DiscoverySearchFilter filter : CollectionUtils.emptyIfNull(searchFilters)) {
            if (StringUtils.equals(name, filter.getIndexFieldName())) {
                return filter;
            }
        }
        return null;
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

    public void setDefaultRpp(int defaultRpp) {
        this.defaultRpp = defaultRpp;
    }

    public int getDefaultRpp() {
        return defaultRpp;
    }

    public void setHitHighlightingConfiguration(DiscoveryHitHighlightingConfiguration hitHighlightingConfiguration) {
        this.hitHighlightingConfiguration = hitHighlightingConfiguration;
    }

    public DiscoveryHitHighlightingConfiguration getHitHighlightingConfiguration() {
        return hitHighlightingConfiguration;
    }

    public void setMoreLikeThisConfiguration(DiscoveryMoreLikeThisConfiguration moreLikeThisConfiguration) {
        this.moreLikeThisConfiguration = moreLikeThisConfiguration;
    }

    public DiscoveryMoreLikeThisConfiguration getMoreLikeThisConfiguration() {
        return moreLikeThisConfiguration;
    }

    public boolean isSpellCheckEnabled() {
        return spellCheckEnabled;
    }

    public void setSpellCheckEnabled(boolean spellCheckEnabled) {
        this.spellCheckEnabled = spellCheckEnabled;
    }

    /**
     * After all the properties are set check that the sidebar facets are a subset of our search filters
     *
     * @throws Exception throws an exception if this isn't the case
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Collection missingSearchFilters = CollectionUtils.subtract(getSidebarFacets(), getSearchFilters());
        if (CollectionUtils.isNotEmpty(missingSearchFilters)) {
            StringBuilder error = new StringBuilder();
            error.append("The following sidebar facet configurations are not present in the search filters list: ");
            for (Object missingSearchFilter : missingSearchFilters) {
                DiscoverySearchFilter searchFilter = (DiscoverySearchFilter) missingSearchFilter;
                error.append(searchFilter.getIndexFieldName()).append(" ");

            }
            error.append("all the sidebar facets MUST be a part of the search filters list.");

            throw new DiscoveryConfigurationException(error.toString());
        }

        Collection missingTagCloudSearchFilters = CollectionUtils
            .subtract(getTagCloudFacetConfiguration().getTagCloudFacets(), getSearchFilters());
        if (CollectionUtils.isNotEmpty(missingTagCloudSearchFilters)) {
            StringBuilder error = new StringBuilder();
            error.append("The following tagCloud facet configurations are not present in the search filters list: ");
            for (Object missingSearchFilter : missingTagCloudSearchFilters) {
                DiscoverySearchFilter searchFilter = (DiscoverySearchFilter) missingSearchFilter;
                error.append(searchFilter.getIndexFieldName()).append(" ");

            }
            error.append("all the tagCloud facets MUST be a part of the search filters list.");

            throw new DiscoveryConfigurationException(error.toString());
        }
    }

    public DiscoverySearchFilterFacet getSidebarFacet(final String facetName) {
        for (DiscoverySearchFilterFacet sidebarFacet : sidebarFacets) {
            if (StringUtils.equals(sidebarFacet.getIndexFieldName(), facetName)) {
                return sidebarFacet;
            }
        }
        return null;
    }
}
