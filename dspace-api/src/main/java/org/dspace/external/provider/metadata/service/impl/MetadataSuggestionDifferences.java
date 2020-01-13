/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.InProgressSubmission;

public class MetadataSuggestionDifferences {

    private Map<String, MetadataSuggestionDifference> differences = new HashMap<>();

    private String suggestionName;
    private InProgressSubmission inProgressSubmission;
    private String id;

    public MetadataSuggestionDifferences(String suggestionName, InProgressSubmission inProgressSubmission,
                                         String entryId) {
        this.suggestionName = suggestionName;
        this.inProgressSubmission = inProgressSubmission;
        this.id = entryId;
    }

    /**
     * Generic getter for the differences
     * @return the differences value of this MetadataSuggestionDifferences
     */
    public Map<String, MetadataSuggestionDifference> getDifferences() {
        return differences;
    }

    /**
     * Generic setter for the differences
     * @param differences   The differences to be set on this MetadataSuggestionDifferences
     */
    public void setDifferences(
        Map<String, MetadataSuggestionDifference> differences) {
        this.differences = differences;
    }

    public MetadataSuggestionDifference getDifference(String metadataKey) {
        return differences.get(metadataKey);
    }

    public void addDifference(String metadataKey, MetadataSuggestionDifference metadataSuggestionDifference) {
        differences.put(metadataKey, metadataSuggestionDifference);
    }

    /**
     * Generic getter for the suggestionName
     * @return the suggestionName value of this MetadataSuggestionDifferences
     */
    public String getSuggestionName() {
        return suggestionName;
    }

    /**
     * Generic setter for the suggestionName
     * @param suggestionName   The suggestionName to be set on this MetadataSuggestionDifferences
     */
    public void setSuggestionName(String suggestionName) {
        this.suggestionName = suggestionName;
    }

    /**
     * Generic getter for the inProgressSubmission
     * @return the inProgressSubmission value of this MetadataSuggestionDifferences
     */
    public InProgressSubmission getInProgressSubmission() {
        return inProgressSubmission;
    }

    /**
     * Generic setter for the inProgressSubmission
     * @param inProgressSubmission   The inProgressSubmission to be set on this MetadataSuggestionDifferences
     */
    public void setInProgressSubmission(InProgressSubmission inProgressSubmission) {
        this.inProgressSubmission = inProgressSubmission;
    }

    /**
     * Generic getter for the id
     * @return the id value of this MetadataSuggestionDifferences
     */
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this MetadataSuggestionDifferences
     */
    public void setId(String id) {
        this.id = id;
    }
}
