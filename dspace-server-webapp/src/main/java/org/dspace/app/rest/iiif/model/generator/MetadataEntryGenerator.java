/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.MetadataEntry;

/**
 * Wraps the domain model metadata property.
 */
public class MetadataEntryGenerator implements IIIFValue {

    private String field;
    private String value;

    /**
     * Set metadata field name.
     * @param field field name
     */
    public MetadataEntryGenerator setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * Set metadata value.
     * @param value metadata value
     */
    public MetadataEntryGenerator setValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public MetadataEntry generate() {
        return new MetadataEntry(field, value);
    }
}
