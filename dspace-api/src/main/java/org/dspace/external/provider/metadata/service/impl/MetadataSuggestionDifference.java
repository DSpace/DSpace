/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

import java.util.List;

/**
 * This class holds a representation of the list of String values that represents the current values for a certain
 * metadata field and the list of metadata changes that can be made to this list
 */
public class MetadataSuggestionDifference {

    /**
     * List of String values that represent the current values of a metadatafield's values
     */
    private List<String> currentValues;
    /**
     * The list of MetadataChanges that can be made to the metadatafield's values
     */
    private List<MetadataChange> metadataChanges;

    /**
     * Generic getter for the currentValues
     * @return the currentValues value of this MetadataSuggestionDifferences
     */
    public List<String> getCurrentValues() {
        return currentValues;
    }

    /**
     * Generic setter for the currentValues
     * @param currentValues   The currentValues to be set on this MetadataSuggestionDifferences
     */
    public void setCurrentValues(List<String> currentValues) {
        this.currentValues = currentValues;
    }

    /**
     * Generic getter for the metadataChanges
     * @return the metadataChanges value of this MetadataSuggestionDifferences
     */
    public List<MetadataChange> getMetadataChanges() {
        return metadataChanges;
    }

    /**
     * Generic setter for the metadataChanges
     * @param metadataChanges   The metadataChanges to be set on this MetadataSuggestionDifferences
     */
    public void setMetadataChanges(List<MetadataChange> metadataChanges) {
        this.metadataChanges = metadataChanges;
    }

    /**
     * This method adds a MetadataChange object to the list of MetadataChanges for this MetadataSuggestionDifference
     * @param metadataChange    The MetadataChange object to be added
     */
    public void addMetadataChange(MetadataChange metadataChange) {
        metadataChanges.add(metadataChange);
    }
}
