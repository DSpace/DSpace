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

/**
 * This class acts as the REST representation for the
 * {@link org.dspace.external.provider.metadata.service.impl.MetadataChange} object. The only difference is that this
 * object contains a list of values instead of a singular value
 */
public class MetadataChangeEntryRest {

    /**
     * The operation for the MetadataChange
     */
    private List<String> operations = new LinkedList<>();
    /**
     * The path to the metadata change
     */
    @JsonIgnore
    private String metadataKey;
    /**
     * The list of values
     */
    @JsonProperty("newvalue")
    private String value;

    /**
     * Constructor for this object
     * @param operation     The operation
     * @param metadataKey   The metadata key
     * @param value         The value
     */
    public MetadataChangeEntryRest(String operation, String metadataKey, String value) {
        operations.add(operation);
        this.metadataKey = metadataKey;
        this.value = value;
    }

    /**
     * Generic getter for the operations
     * @return the operations value of this MetadataChangeEntryRest
     */
    public List<String> getOperations() {
        return operations;
    }

    /**
     * Generic setter for the operations
     * @param operations   The operations to be set on this MetadataChangeEntryRest
     */
    public void setOperations(List<String> operations) {
        this.operations = operations;
    }

    /**
     * Generic getter for the metadataKey
     * @return the metadataKey value of this MetadataChangeEntryRest
     */
    public String getMetadataKey() {
        return metadataKey;
    }

    /**
     * Generic setter for the metadataKey
     * @param metadataKey   The metadataKey to be set on this MetadataChangeEntryRest
     */
    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    /**
     * Generic getter for the value
     * @return the value value of this MetadataChangeEntryRest
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this MetadataChangeEntryRest
     */
    public void setValue(String value) {
        this.value = value;
    }
}
