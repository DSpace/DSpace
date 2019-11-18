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
    private String op;
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
     * @param op            The operation
     * @param metadataKey   The metadata key
     * @param value         The value
     */
    public MetadataChange(String op, String metadataKey, String value) {
        this.op = op;
        this.metadataKey = metadataKey;
        this.value = value;
    }

    /**
     * Generic getter for the op
     * @return the op value of this MetadataChange
     */
    public String getOp() {
        return op;
    }

    /**
     * Generic setter for the op
     * @param op   The op to be set on this MetadataChange
     */
    public void setOp(String op) {
        this.op = op;
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
