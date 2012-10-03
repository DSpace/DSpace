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
    private int offset=-1;
    /* The facet prefix, all facet values will have to start with the given prefix */
    private String prefix;
    private String type;
    private DiscoveryConfigurationParameters.SORT sortOrder;

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder) {
        this.field = field;
        this.type = type;
        this.limit = limit;
        this.sortOrder = sortOrder;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder, int offset) {
        this.field = field;
        this.type = type;
        this.limit = limit;
        this.sortOrder = sortOrder;
        this.offset = offset;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder, String prefix) {
        this.prefix = prefix;
        this.limit = limit;
        this.type = type;
        this.sortOrder = sortOrder;
        this.field = field;
    }

    public DiscoverFacetField(String field, String type, int limit, DiscoveryConfigurationParameters.SORT sortOrder, String prefix, int offset) {
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
    
    public int getOffset()
    {
        return offset;
    }
    
    public void setOffset(int offset)
    {
        this.offset = offset;
    }
}
