/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.dspace.app.rest.MetadataSuggestionsRestController;

/**
 * This class acts as a REST wrapper for the {@link MetadataChangeEntryRest} objects
 */
public class MetadataChangeRest extends BaseObjectRest<String> {

    /**
     * This list of {@link MetadataChangeEntryRest} objects
     */
    private List<MetadataChangeEntryRest> metadataChangeEntryRests;

    public static final String NAME = "metadataChangeRest";

    @Override
    public String getCategory() {
        return INTEGRATION;
    }

    @Override
    public Class getController() {
        return MetadataSuggestionsRestController.class;
    }

    /**
     * Generic getter for the metadataChangeEntryRests
     * @return the metadataChangeEntryRests value of this MetadataChangeRest
     */
    @JsonValue
    public List<MetadataChangeEntryRest> getMetadataChangeEntryRests() {
        return metadataChangeEntryRests;
    }

    /**
     * Generic setter for the metadataChangeEntryRests
     * @param metadataChangeEntryRests   The metadataChangeEntryRests to be set on this MetadataChangeRest
     */
    public void setMetadataChangeEntryRests(List<MetadataChangeEntryRest> metadataChangeEntryRests) {
        this.metadataChangeEntryRests = metadataChangeEntryRests;
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
