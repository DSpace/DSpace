/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.filter.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.external.provider.metadata.filter.MetadataSuggestionProviderFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is an implementation of {@link MetadataSuggestionProviderFilter} and checks whether the
 * MetadataValue for a given MetadataField is equal to the given value passed along through the bean
 */
public class MetadataSuggestionProviderEqualityFilter implements MetadataSuggestionProviderFilter {

    @Autowired
    private ItemService itemService;

    private String metadataFieldString;
    private String value;

    /**
     * Generic getter for the metadataFieldString
     * @return the metadataFieldString value of this MetadataSuggestionProviderEqualityFilter
     */
    public String getMetadataFieldString() {
        return metadataFieldString;
    }

    /**
     * Generic setter for the metadataFieldString
     * @param metadataFieldString   The metadataFieldString to be set on this MetadataSuggestionProviderEqualityFilter
     */
    public void setMetadataFieldString(String metadataFieldString) {
        this.metadataFieldString = metadataFieldString;
    }

    /**
     * Generic getter for the value
     * @return the value value of this MetadataSuggestionProviderEqualityFilter
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this MetadataSuggestionProviderEqualityFilter
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean supports(InProgressSubmission inProgressSubmission) {
        List<MetadataValue> metadata = itemService
            .getMetadataByMetadataString(inProgressSubmission.getItem(), metadataFieldString);
        for (MetadataValue metadataValue : metadata) {
            if (StringUtils.equalsIgnoreCase(metadataValue.getValue(), value)) {
                return true;
            }
        }
        return false;
    }
}
