/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * Special sidebar facet configuration used for Graph facets
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
public class GraphDiscoverSearchFilterFacet extends DiscoverySearchFilterFacet {

    public static final String TYPE_PREFIX = "chart.";

    private int facetLimit = -1;
    private DiscoveryConfigurationParameters.SORT sortOrderSidebar = DiscoveryConfigurationParameters.SORT.COUNT;
    private String graphType;
    private String splitter;
    private boolean onlyLastNodeRelevant = false;
    private boolean isDate = false;
    private int maxLevels = Integer.MAX_VALUE;
    @Override
    public String getType() {
        return TYPE_PREFIX + graphType;
    }

    public String getGraphType() {
        return graphType;
    }
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }

    public void setIndexFieldName(String indexFieldName) {
        this.indexFieldName = indexFieldName;
    }
    public int getFacetLimit() {
        return facetLimit;
    }
    public void setFacetLimit(int facetLimit) {
        this.facetLimit = facetLimit;
    }
    public DiscoveryConfigurationParameters.SORT getSortOrderSidebar() {
        return sortOrderSidebar;
    }
    public void setSortOrderSidebar(DiscoveryConfigurationParameters.SORT sortOrderSidebar) {
        this.sortOrderSidebar = sortOrderSidebar;
    }
    public void setSplitter(String splitter) {
        this.splitter = splitter;
    }
    public void setOnlyLastNodeRelevant(boolean onlyLastNodeRelevant) {
        this.onlyLastNodeRelevant = onlyLastNodeRelevant;
    }
    public void setIsDate(boolean isDate) {
        this.isDate = isDate;
    }

    public String getSplitter() {
        return splitter;
    }

    public boolean isDate() {
        return isDate;
    }

    public boolean isOnlyLastNodeRelevant() {
        return onlyLastNodeRelevant;
    }

    public int getMaxLevels() {
        return maxLevels;
    }

    public void setMaxLevels(int max) {
        this.maxLevels = max;
    }

}
