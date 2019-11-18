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

/**
 * This class acts as the REST representation for the
 * {@link org.dspace.external.provider.metadata.service.impl.MetadataChange} object. The only difference is that this
 * object contains a list of values instead of a singular value
 */
public class MetadataChangeEntryRest {

    /**
     * The operation for the MetadataChange
     */
    private String op;
    /**
     * The path to the metadata change
     */
    private String path;
    /**
     * The list of values
     */
    private List<String> values;

    /**
     * Constructor for this object
     * @param op            The operation
     * @param metadataKey   The metadata key
     * @param value         The value
     */
    public MetadataChangeEntryRest(String op, String metadataKey, String value) {
        this.op = op;
        this.path = metadataKey;
        this.values = new LinkedList<>();
        values.add(value);
    }

    /**
     * Generic getter for the op
     * @return the op value of this MetadataChangeEntryRest
     */
    public String getOp() {
        return op;
    }

    /**
     * Generic setter for the op
     * @param op   The op to be set on this MetadataChangeEntryRest
     */
    public void setOp(String op) {
        this.op = op;
    }

    /**
     * Generic getter for the path
     * @return the path value of this MetadataChangeEntryRest
     */
    public String getPath() {
        return path;
    }

    /**
     * Generic setter for the path
     * @param path   The path to be set on this MetadataChangeEntryRest
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Generic getter for the values
     * @return the values value of this MetadataChangeEntryRest
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * Generic setter for the values
     * @param values   The values to be set on this MetadataChangeEntryRest
     */
    public void setValues(List<String> values) {
        this.values = values;
    }

    public void addValue(String value) {
        if (values == null) {
            values = new LinkedList<>();
        }
        values.add(value);
    }
}
