/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * REST representation of the {@link org.dspace.external.provider.metadata.MetadataSuggestionProvider}
 */
public class MetadataSuggestionsSourceRest extends BaseObjectRest<String> {

    public static final String NAME = "metadatasuggestion";
    public static final String PLURAL_NAME = "metadatasuggestions";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    /**
     * This id for the MetadataSuggestionsSouceRest object
     */
    private String id;
    /**
     * The name for the MetadataSuggestionsSouceRest object
     */
    private String name;
    /**
     * A boolean indicating whether this MetadataSuggestionsSouceRest object is query based or not
     */
    @JsonProperty("query-based")
    private boolean queryBased;
    /**k
     * A boolean indicating whether this MetadataSuggestionsSouceRest object is file based or not
     */
    @JsonProperty("file-based")
    private boolean fileBased;
    /**
     * A boolean indicating whethe this MetadataSuggestionsSouceRest object is metadata based or not
     */
    @JsonProperty("metadata-based")
    private boolean metadataBased;

    /**
     * Generic getter for the id
     * @return the id value of this MetadataSuggestionsSourceRest
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Generic setter for the id
     * @param id   The id to be set on this MetadataSuggestionsSourceRest
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Generic getter for the name
     * @return the name value of this MetadataSuggestionsSourceRest
     */
    public String getName() {
        return name;
    }

    /**
     * Generic setter for the name
     * @param name   The name to be set on this MetadataSuggestionsSourceRest
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Generic getter for the queryBased
     * @return the queryBased value of this MetadataSuggestionsSourceRest
     */
    public boolean isQueryBased() {
        return queryBased;
    }

    /**
     * Generic setter for the queryBased
     * @param queryBased   The queryBased to be set on this MetadataSuggestionsSourceRest
     */
    public void setQueryBased(boolean queryBased) {
        this.queryBased = queryBased;
    }

    /**
     * Generic getter for the fileBased
     * @return the fileBased value of this MetadataSuggestionsSourceRest
     */
    public boolean isFileBased() {
        return fileBased;
    }

    /**
     * Generic setter for the fileBased
     * @param fileBased   The fileBased to be set on this MetadataSuggestionsSourceRest
     */
    public void setFileBased(boolean fileBased) {
        this.fileBased = fileBased;
    }

    /**
     * Generic getter for the metadataBased
     * @return the metadataBased value of this MetadataSuggestionsSourceRest
     */
    public boolean isMetadataBased() {
        return metadataBased;
    }

    /**
     * Generic setter for the metadataBased
     * @param metadataBased   The metadataBased to be set on this MetadataSuggestionsSourceRest
     */
    public void setMetadataBased(boolean metadataBased) {
        this.metadataBased = metadataBased;
    }
}
