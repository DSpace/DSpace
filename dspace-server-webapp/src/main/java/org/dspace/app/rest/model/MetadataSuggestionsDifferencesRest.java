/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.MetadataSuggestionsRestController;

/**
 * This is the REST object to represent the MetadataSuggestionsDifferences through the use of a Map containing the
 * String MetadataFieldKey and the MetadataDifferenceRest object as value
 */
public class MetadataSuggestionsDifferencesRest extends BaseObjectRest<String> {

    public static final String NAME = "metadataSuggestionDifference";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;


    /**
     * The map that holds the metadata field String representation as key and the MetadataDifferenceRest object as value
     */
    private Map<String, MetadataDifferenceRest> differences;

    @JsonIgnore
    private String metadataSuggestion;

    @JsonIgnore
    private Integer workspaceItemId;
    @JsonIgnore
    private Integer workflowItemId;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return MetadataSuggestionsRestController.class;
    }

    @Override
    @JsonIgnore
    public String getType() {
        return NAME;
    }

    @JsonIgnore
    @Override
    public String getId() {
        return id;
    }

    /**
     * Generic getter for the differences
     * @return the differences value of this MetadataSuggestionsDifferencesRest
     */
    public Map<String, MetadataDifferenceRest> getDifferences() {
        return differences;
    }

    /**
     * Generic setter for the differences
     * @param differences   The differences to be set on this MetadataSuggestionsDifferencesRest
     */
    public void setDifferences(
        Map<String, MetadataDifferenceRest> differences) {
        this.differences = differences;
    }

    /**
     * Generic getter for the metadataSuggestion
     * @return the metadataSuggestion value of this MetadataSuggestionsDifferencesRest
     */
    public String getMetadataSuggestion() {
        return metadataSuggestion;
    }

    /**
     * Generic setter for the metadataSuggestion
     * @param metadataSuggestion   The metadataSuggestion to be set on this MetadataSuggestionsDifferencesRest
     */
    public void setMetadataSuggestion(String metadataSuggestion) {
        this.metadataSuggestion = metadataSuggestion;
    }

    /**
     * Generic getter for the workspaceItemId
     * @return the workspaceItemId value of this MetadataSuggestionsDifferencesRest
     */
    public Integer getWorkspaceItemId() {
        return workspaceItemId;
    }

    /**
     * Generic setter for the workspaceItemId
     * @param workspaceItemId   The workspaceItemId to be set on this MetadataSuggestionsDifferencesRest
     */
    public void setWorkspaceItemId(Integer workspaceItemId) {
        this.workspaceItemId = workspaceItemId;
    }

    /**
     * Generic getter for the workflowItemId
     * @return the workflowItemId value of this MetadataSuggestionsDifferencesRest
     */
    public Integer getWorkflowItemId() {
        return workflowItemId;
    }

    /**
     * Generic setter for the workflowItemId
     * @param workflowItemId   The workflowItemId to be set on this MetadataSuggestionsDifferencesRest
     */
    public void setWorkflowItemId(Integer workflowItemId) {
        this.workflowItemId = workflowItemId;
    }
}
