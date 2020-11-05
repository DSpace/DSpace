/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.StringUtils;

/**
 * Special sidebar facet configuration used for Graph facets
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4Science.it)
 */
public class GraphDiscoverSearchFilterFacet extends DiscoverySearchFilterFacet {

    public static final String TYPE_PREFIX = "chart.";

    private int facetLimit = -1;
    private DiscoveryConfigurationParameters.SORT sortOrderSidebar = DiscoveryConfigurationParameters.SORT.COUNT;
    private DiscoverySearchFilterFacet dataFacet;
    private String graphType;

    @Override
    public String getIndexFieldName() {
        if (StringUtils.equals(dataFacet.getType(), "date")) {
            return getType() + "." + dataFacet.getIndexFieldName() + ".year";
        } else {
            return getType() + "." + dataFacet.getIndexFieldName() + "_filter";
        }
    }

    @Override
    public String getType() {
        return TYPE_PREFIX + graphType;
    }

    @Override
    public List<String> getMetadataFields() {
        return Collections.emptyList();
    }

    public DiscoverySearchFilterFacet getDataFacet() {
        return dataFacet;
    }

    public void setDataFacet(DiscoverySearchFilterFacet dataFacet) {
        this.dataFacet = dataFacet;
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
}
