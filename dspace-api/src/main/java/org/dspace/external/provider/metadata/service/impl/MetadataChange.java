/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.service.impl;

/**
 * This class represents a change in metadata
 */
public class MetadataChange {

    /**
     * A String representing the type of change
     */
    private String operation;
    /**
     * A String representing the metadata key
     */
    private String metadataKey;
    /**
     * A String representing the value
     */
    private String value;

    /**
     * Constructor for this object
     * @param operation     The operation
     * @param metadataKey   The metadata key
     * @param value         The value
     */
    public MetadataChange(String operation, String metadataKey, String value) {
        this.operation = operation;
        this.metadataKey = metadataKey;
        this.value = value;
    }

    /**
     * Generic getter for the operation
     * @return the operation value of this MetadataChange
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Generic setter for the operation
     * @param operation   The operation to be set on this MetadataChange
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * Generic getter for the metadataKey
     * @return the metadataKey value of this MetadataChange
     */
    public String getMetadataKey() {
        return metadataKey;
    }

    /**
     * Generic setter for the metadataKey
     * @param metadataKey   The metadataKey to be set on this MetadataChange
     */
    public void setMetadataKey(String metadataKey) {
        this.metadataKey = metadataKey;
    }

    /**
     * Generic getter for the value
     * @return the value value of this MetadataChange
     */
    public String getValue() {
        return value;
    }

    /**
     * Generic setter for the value
     * @param value   The value to be set on this MetadataChange
     */
    public void setValue(String value) {
        this.value = value;
    }
}
