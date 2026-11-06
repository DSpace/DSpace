/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.dspace.discovery.configuration.DiscoveryConfigurationParameters;

/**
 * Class contains facet query information
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverFacetField {
    private String field;
    private int limit;
    private int offset = -1;
    /* The facet prefix, all facet values will have to start with the given prefix */
    private String prefix;
    private String type;
    private DiscoveryConfigurationParameters.SORT sortOrder;
    private boolean exposeMore;
    private boolean exposeMissing;
    private boolean exposeTotalElements;
    private boolean fillGaps;
    private boolean inverseDirection;

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder) {
        this.field = field;
        this.type = type;
        this.limit = limit;
        this.sortOrder = sortOrder;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder,
            int offset) {
        this.field = field;
        this.type = type;
        this.limit = limit;
        this.sortOrder = sortOrder;
        this.offset = offset;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder,
            String prefix) {
        this.prefix = prefix;
        this.limit = limit;
        this.type = type;
        this.sortOrder = sortOrder;
        this.field = field;
    }

    /**
     * Creates a facet field query exposing the additional facet metadata options.
     *
     * @param field               the facet field name
     * @param type                the facet type
     * @param limit               the maximum number of facet values to return
     * @param sortOrder           the sort order to apply to the facet values
     * @param prefix              the prefix all returned facet values must start with
     * @param exposeMore          whether to expose the information about additional available values
     * @param exposeMissing       whether to expose the count of documents missing the facet value
     * @param exposeTotalElements whether to expose the total number of distinct facet values
     * @param fillGaps            whether to fill the gaps between date facet values
     * @param inverseDirection    whether to invert the facet direction
     */
    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder,
                              String prefix, boolean exposeMore, boolean exposeMissing, boolean exposeTotalElements,
                              boolean fillGaps, boolean inverseDirection) {
        this.prefix = prefix;
        this.limit = limit;
        this.type = type;
        this.sortOrder = sortOrder;
        this.field = field;
        this.exposeMore = exposeMore;
        this.exposeMissing = exposeMissing;
        this.exposeTotalElements = exposeTotalElements;
        this.fillGaps = fillGaps;
        this.inverseDirection = inverseDirection;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder,
                              String prefix, int offset) {
        this.prefix = prefix;
        this.limit = limit;
        this.type = type;
        this.sortOrder = sortOrder;
        this.field = field;
        this.offset = offset;
    }

    public String getField() {
        return field;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getLimit() {
        return limit;
    }

    public String getType() {
        return type;
    }

    public DiscoveryConfigurationParameters.SORT getSortOrder() {
        return sortOrder;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Returns whether the count of documents missing the facet value should be exposed.
     *
     * @return {@code true} if the missing count should be exposed
     */
    public boolean exposeMissing() {
        return exposeMissing;
    }

    /**
     * Returns whether the information about additional available facet values should be exposed.
     *
     * @return {@code true} if the "more" information should be exposed
     */
    public boolean exposeMore() {
        return exposeMore;
    }

    /**
     * Returns whether the total number of distinct facet values should be exposed.
     *
     * @return {@code true} if the total elements count should be exposed
     */
    public boolean exposeTotalElements() {
        return exposeTotalElements;
    }

    /**
     * Returns whether the gaps between date facet values should be filled.
     *
     * @return {@code true} if date gaps should be filled
     */
    public boolean fillGaps() {
        return fillGaps;
    }

    /**
     * Returns whether the facet direction should be inverted.
     *
     * @return {@code true} if the direction should be inverted
     */
    public boolean inverseDirection() {
        return inverseDirection;
    }
}
