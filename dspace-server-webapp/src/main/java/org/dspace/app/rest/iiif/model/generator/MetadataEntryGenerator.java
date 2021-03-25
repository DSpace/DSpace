/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.MetadataEntry;
import org.springframework.stereotype.Component;

@Component
public class MetadataEntryGenerator implements IIIFValue {

    private String field;
    private String value;

    public void setField(String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public MetadataEntry getValue() {
        return new MetadataEntry(field, value);
    }
}
