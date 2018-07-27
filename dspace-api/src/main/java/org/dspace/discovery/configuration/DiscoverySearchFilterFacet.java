/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * An expanded class that allows a search filter to be used as a sidebar facet
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoverySearchFilterFacet extends DiscoverySearchFilter {

    private static final int DEFAULT_FACET_LIMIT = 10;
    private int facetLimit = -1;
    private DiscoveryConfigurationParameters.SORT sortOrderSidebar = DiscoveryConfigurationParameters.SORT.COUNT;
    private DiscoveryConfigurationParameters.SORT sortOrderFilterPage = DiscoveryConfigurationParameters.SORT.COUNT;
    public static final String FILTER_TYPE_FACET = "facet";
    private boolean exposeMinMax = false;

    public int getFacetLimit() {
        if (facetLimit == -1) {
            return DEFAULT_FACET_LIMIT;
        } else {
            return facetLimit;
        }
    }

    public void setFacetLimit(int facetLimit) {
        this.facetLimit = facetLimit;
    }

    public DiscoveryConfigurationParameters.SORT getSortOrderFilterPage() {
        return sortOrderFilterPage;
    }

    public void setSortOrderFilterPage(DiscoveryConfigurationParameters.SORT sortOrderFilterPage) {
        this.sortOrderFilterPage = sortOrderFilterPage;
    }

    public DiscoveryConfigurationParameters.SORT getSortOrderSidebar() {
        return sortOrderSidebar;
    }

    public void setSortOrderSidebar(DiscoveryConfigurationParameters.SORT sortOrderSidebar) {
        this.sortOrderSidebar = sortOrderSidebar;
    }

    @Override
    public String getFilterType() {
        return FILTER_TYPE_FACET;
    }

    /**
     * This is a boolean value indicating whether or not this DiscoverySearchFilterFacet should expose
     * the minimum and maximum value
     *
     * @return A boolean indicating whether or not the minimum and maximum value should be exposed
     */
    public boolean isExposeMinMax() {
        return exposeMinMax;
    }

    /**
     * This method will set the exposeMinMax property to the boolean that's passed along with the param
     *
     * @param exposeMinMax The boolean that will the exposeMinMax property will be set to
     */
    public void setExposeMinMax(boolean exposeMinMax) {
        this.exposeMinMax = exposeMinMax;
    }
}
