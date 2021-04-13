/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class DiscoverySortConfiguration {

    public static final String SCORE = "score";

    private List<DiscoverySortFieldConfiguration> sortFields = new ArrayList<DiscoverySortFieldConfiguration>();

    public List<DiscoverySortFieldConfiguration> getSortFields() {
        return sortFields;
    }

    public void setSortFields(List<DiscoverySortFieldConfiguration> sortFields) {
        this.sortFields = sortFields;
    }

    public DiscoverySortFieldConfiguration getSortFieldConfiguration(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return null;
        }

        if (StringUtils.equalsIgnoreCase(SCORE, sortField)) {
            DiscoverySortFieldConfiguration configuration = new DiscoverySortFieldConfiguration();
            configuration.setMetadataField(SCORE);
            return configuration;
        }

        for (DiscoverySortFieldConfiguration sortFieldConfiguration : CollectionUtils.emptyIfNull(sortFields)) {
            if (StringUtils.equals(sortFieldConfiguration.getMetadataField(), sortField)) {
                return sortFieldConfiguration;
            }
        }
        return null;
    }
}
