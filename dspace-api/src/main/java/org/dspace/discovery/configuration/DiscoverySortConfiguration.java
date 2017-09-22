/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySortConfiguration {

    /** Attributes used for sorting of results **/
    public enum SORT_ORDER {
        desc,
        asc
    }

    private DiscoverySortFieldConfiguration defaultSort = null;

    private List<DiscoverySortFieldConfiguration> sortFields = new ArrayList<DiscoverySortFieldConfiguration>();

    private SORT_ORDER defaultSortOrder = SORT_ORDER.desc;

    public DiscoverySortFieldConfiguration getDefaultSort() {
        return defaultSort;
    }

    public void setDefaultSort(DiscoverySortFieldConfiguration defaultSort) {
        this.defaultSort = defaultSort;
    }

    public List<DiscoverySortFieldConfiguration> getSortFields() {
        return sortFields;
    }

    public void setSortFields(List<DiscoverySortFieldConfiguration> sortFields) {
        this.sortFields = sortFields;
    }

    public SORT_ORDER getDefaultSortOrder() {
        return defaultSortOrder;
    }

    public void setDefaultSortOrder(SORT_ORDER defaultSortOrder) {
        this.defaultSortOrder = defaultSortOrder;
    }
    
    public boolean isValidSortField(String sortField) {
        if(StringUtils.equals("score", sortField)) {
            return true;
        }
        for (DiscoverySortFieldConfiguration sortFieldConfiguration : CollectionUtils.emptyIfNull(sortFields)) {
            if(StringUtils.equals(sortFieldConfiguration.getMetadataField(), sortField)) {
                return true;
            }
        }
        return false;
    }
}
