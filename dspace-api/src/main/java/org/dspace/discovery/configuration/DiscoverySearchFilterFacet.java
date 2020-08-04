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
     * This method returns whether or not the DiscoverySearchFilterFacet should return a
     * min and max value.
     *
     * @return  A boolean indicating whether or not this DiscoverySearchFilterFacet should expose
     *          a min and max value
     */
    public boolean exposeMinAndMaxValue() {
        return exposeMinMax;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#exposeMinAndMaxValue}
     * @param exposeMinMax A boolean value that will be set to return in the above mentioned link
     */
    public void setExposeMinAndMaxValue(boolean exposeMinMax) {
        this.exposeMinMax = exposeMinMax;
    }
}
