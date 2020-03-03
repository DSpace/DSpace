/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.MetadataSuggestionsRestController;

/**
 * REST respresentation for the MetadataSuggestions
 */
public class MetadataSuggestionEntryRest extends BaseObjectRest<String> {

    public static final String NAME = "metadataSuggestionEntry";
    public static final String PLURAL_NAME = "metadataSuggestionEntries";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private String id;
    private String display;
    private String value;
    private String metadataSuggestion;

    @JsonProperty("metadata")
    private MetadataRest metadataRest;

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
    public String getType() {
        return NAME;
    }

    /**
     * Generic getter for the id
     * @return the id value of this MetadataSuggestionEntryRest
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this MetadataSuggestionEntryRest
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the display
     * @return the display value of this MetadataSuggestionEntryRest
     */
    public String getDisplay() {
        return display;
    }

    /**
     * Generic setter for the display
     * @param display   The display to be set on this MetadataSuggestionEntryRest
     */
    public void setDisplay(String display) {
        this.display = display;
    }

    /**
     * Generic getter for the value
     * @return the value value of this MetadataSuggestionEntryRest
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this MetadataSuggestionEntryRest
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Generic getter for the metadataSuggestion
     * @return the metadataSuggestion value of this MetadataSuggestionEntryRest
     */
    public String getMetadataSuggestion() {
        return metadataSuggestion;
    }

    /**
     * Generic setter for the metadataSuggestion
     * @param metadataSuggestion   The metadataSuggestion to be set on this MetadataSuggestionEntryRest
     */
    public void setMetadataSuggestion(String metadataSuggestion) {
        this.metadataSuggestion = metadataSuggestion;
    }

    /**
     * Generic getter for the metadataRest
     * @return the metadataRest value of this MetadataSuggestionEntryRest
     */
    public MetadataRest getMetadataRest() {
        return metadataRest;
    }

    /**
     * Generic setter for the metadataRest
     * @param metadataRest   The metadataRest to be set on this MetadataSuggestionEntryRest
     */
    public void setMetadataRest(MetadataRest metadataRest) {
        this.metadataRest = metadataRest;
    }

    /**
     * Generic getter for the workspaceItemId
     * @return the workspaceItemId value of this MetadataSuggestionEntryRest
     */
    public Integer getWorkspaceItemId() {
        return workspaceItemId;
    }

    /**
     * Generic setter for the workspaceItemId
     * @param workspaceItemId   The workspaceItemId to be set on this MetadataSuggestionEntryRest
     */
    public void setWorkspaceItemId(Integer workspaceItemId) {
        this.workspaceItemId = workspaceItemId;
    }

    /**
     * Generic getter for the workflowItemId
     * @return the workflowItemId value of this MetadataSuggestionEntryRest
     */
    public Integer getWorkflowItemId() {
        return workflowItemId;
    }

    /**
     * Generic setter for the workflowItemId
     * @param workflowItemId   The workflowItemId to be set on this MetadataSuggestionEntryRest
     */
    public void setWorkflowItemId(Integer workflowItemId) {
        this.workflowItemId = workflowItemId;
    }
}
