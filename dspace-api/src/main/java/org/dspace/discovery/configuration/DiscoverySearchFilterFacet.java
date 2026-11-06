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
    private boolean exposeMissing = true;
    private boolean exposeMore = false;
    private boolean exposeTotalElements = false;
    private boolean fillDateGaps = false;
    private boolean inverseDirection = false;

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
     * This method returns whether the DiscoverySearchFilterFacet should return a
     * min and max value.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should expose
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

    /**
     * This method returns whether the count of documents missing the facet value should be exposed.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should expose
     *          the missing count
     */
    public boolean exposeMissing() {
        return exposeMissing;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#exposeMissing}
     * @param exposeMissing A boolean value that will be set to return in the above mentioned link
     */
    public void setExposeMissing(boolean exposeMissing) {
        this.exposeMissing = exposeMissing;
    }

    /**
     * This method returns whether the information about additional available facet values should be exposed.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should expose
     *          the "more" information
     */
    public boolean exposeMore() {
        return exposeMore;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#exposeMore}
     * @param exposeMore A boolean value that will be set to return in the above mentioned link
     */
    public void setExposeMore(boolean exposeMore) {
        this.exposeMore = exposeMore;
    }

    /**
     * This method returns whether the total number of distinct facet values should be exposed.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should expose
     *          the total elements count
     */
    public boolean exposeTotalElements() {
        return exposeTotalElements;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#exposeTotalElements}
     * @param exposeTotalElements A boolean value that will be set to return in the above mentioned link
     */
    public void setExposeTotalElements(boolean exposeTotalElements) {
        this.exposeTotalElements = exposeTotalElements;
    }

    /**
     * This method returns whether the gaps between date facet values should be filled.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should fill
     *          date gaps
     */
    public boolean fillDateGaps() {
        return fillDateGaps;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#fillDateGaps}
     * @param fillDateGaps A boolean value that will be set to return in the above mentioned link
     */
    public void setFillDateGaps(boolean fillDateGaps) {
        this.fillDateGaps = fillDateGaps;
    }

    /**
     * This method returns whether the facet direction should be inverted.
     *
     * @return  A boolean indicating whether this DiscoverySearchFilterFacet should invert
     *          the direction
     */
    public boolean inverseDirection() {
        return inverseDirection;
    }

    /**
     * This method sets the boolean for {@link org.dspace.discovery.configuration.DiscoverySearchFilterFacet#inverseDirection}
     * @param inverseDirection A boolean value that will be set to return in the above mentioned link
     */
    public void setInverseDirection(boolean inverseDirection) {
        this.inverseDirection = inverseDirection;
    }
}
