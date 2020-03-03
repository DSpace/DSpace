/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.MetadataSuggestionsRestController;

/**
 * This is the REST object to represent a MetadataDifference
 */
public class MetadataDifferenceRest extends BaseObjectRest<String> {

    public static final String NAME = "metadataDifferenceRest";

    private List<String> currentValues = new LinkedList<>();
    private MetadataChangeRest suggestions;

    /**
     * Generic getter for the currentValues
     * @return the currentValues value of this MetadataDifferenceRest
     */
    public List<String> getCurrentValues() {
        return currentValues;
    }

    /**
     * Generic setter for the currentValues
     * @param currentValues   The currentValues to be set on this MetadataDifferenceRest
     */
    public void setCurrentValues(List<String> currentValues) {
        this.currentValues = currentValues;
    }

    /**
     * Generic getter for the suggestions
     * @return the suggestions value of this MetadataDifferenceRest
     */
    public MetadataChangeRest getSuggestions() {
        return suggestions;
    }

    /**
     * Generic setter for the suggestions
     * @param suggestions   The suggestions to be set on this MetadataDifferenceRest
     */
    public void setSuggestions(MetadataChangeRest suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String getCategory() {
        return INTEGRATION;
    }

    @Override
    public Class getController() {
        return MetadataSuggestionsRestController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonIgnore
    public String getType() {
        return NAME;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonIgnore
    public String getId() {
        return id;
    }
}
