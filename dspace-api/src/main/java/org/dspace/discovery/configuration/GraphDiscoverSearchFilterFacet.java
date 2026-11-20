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

    /**
     * Returns the graph type of this facet.
     *
     * @return the graph type
     */
    public String getGraphType() {
        return graphType;
    }

    /**
     * Sets the graph type of this facet.
     *
     * @param graphType the graph type
     */
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }

    /**
     * Sets the Solr index field name used by this facet.
     *
     * @param indexFieldName the index field name
     */
    public void setIndexFieldName(String indexFieldName) {
        this.indexFieldName = indexFieldName;
    }

    /**
     * Returns the maximum number of facet values to return.
     *
     * @return the facet limit
     */
    public int getFacetLimit() {
        return facetLimit;
    }

    /**
     * Sets the maximum number of facet values to return.
     *
     * @param facetLimit the facet limit
     */
    public void setFacetLimit(int facetLimit) {
        this.facetLimit = facetLimit;
    }

    /**
     * Returns the sort order applied to the facet values in the sidebar.
     *
     * @return the sidebar sort order
     */
    public DiscoveryConfigurationParameters.SORT getSortOrderSidebar() {
        return sortOrderSidebar;
    }

    /**
     * Sets the sort order applied to the facet values in the sidebar.
     *
     * @param sortOrderSidebar the sidebar sort order
     */
    public void setSortOrderSidebar(DiscoveryConfigurationParameters.SORT sortOrderSidebar) {
        this.sortOrderSidebar = sortOrderSidebar;
    }

    /**
     * Sets the splitter used to break facet values into graph nodes.
     *
     * @param splitter the splitter
     */
    public void setSplitter(String splitter) {
        this.splitter = splitter;
    }

    /**
     * Sets whether only the last node of a split facet value is relevant.
     *
     * @param onlyLastNodeRelevant {@code true} if only the last node is relevant
     */
    public void setOnlyLastNodeRelevant(boolean onlyLastNodeRelevant) {
        this.onlyLastNodeRelevant = onlyLastNodeRelevant;
    }

    /**
     * Sets whether this facet represents a date.
     *
     * @param isDate {@code true} if this facet is a date facet
     */
    public void setIsDate(boolean isDate) {
        this.isDate = isDate;
    }

    /**
     * Returns the splitter used to break facet values into graph nodes.
     *
     * @return the splitter
     */
    public String getSplitter() {
        return splitter;
    }

    /**
     * Returns whether this facet represents a date.
     *
     * @return {@code true} if this facet is a date facet
     */
    public boolean isDate() {
        return isDate;
    }

    /**
     * Returns whether only the last node of a split facet value is relevant.
     *
     * @return {@code true} if only the last node is relevant
     */
    public boolean isOnlyLastNodeRelevant() {
        return onlyLastNodeRelevant;
    }

    /**
     * Returns the maximum number of node levels considered for this facet.
     *
     * @return the maximum number of levels
     */
    public int getMaxLevels() {
        return maxLevels;
    }

    /**
     * Sets the maximum number of node levels considered for this facet.
     *
     * @param max the maximum number of levels
     */
    public void setMaxLevels(int max) {
        this.maxLevels = max;
    }

}
