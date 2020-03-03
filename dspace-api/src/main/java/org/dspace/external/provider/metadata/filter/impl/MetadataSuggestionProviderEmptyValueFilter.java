/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.filter.impl;

import java.util.List;

import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.external.provider.metadata.filter.MetadataSuggestionProviderFilter;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is an implementation of {@link MetadataSuggestionProviderFilter} and checks that the MetadataValue for
 * the given MetadataField is empty
 */
public class MetadataSuggestionProviderEmptyValueFilter implements MetadataSuggestionProviderFilter {

    @Autowired
    private ItemService itemService;

    private String metadataFieldString;

    /**
     * Generic getter for the metadataFieldString
     * @return the metadataFieldString value of this MetadataSuggestionProviderEmptyValueFilter
     */
    public String getMetadataFieldString() {
        return metadataFieldString;
    }

    /**
     * Generic setter for the metadataFieldString
     * @param metadataFieldString   The metadataFieldString to be set on this MetadataSuggestionProviderEmptyValueFilter
     */
    public void setMetadataFieldString(String metadataFieldString) {
        this.metadataFieldString = metadataFieldString;
    }

    @Override
    public boolean supports(InProgressSubmission inProgressSubmission) {
        List<MetadataValue> metadata = itemService
            .getMetadataByMetadataString(inProgressSubmission.getItem(), metadataFieldString);
        return metadata.isEmpty();
    }
}
