/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.PropertyValue;
import org.dspace.core.I18nUtil;
import org.springframework.stereotype.Component;

@Component
public class MetadataEntryGenerator implements IIIFValue {

    private String field;
    private String value;
    private String[] rest;

    /**
     * Set metadata field name.
     * @param field field name
     */
    public void setField(String field) {
        this.field = field;
    }

    /**
     * Set metadata value.
     * @param value metadata value
     */
    public void setValue(String value, String... rest) {
        this.value = value;
        this.rest = rest;
    }

    @Override
    public MetadataEntry getValue() {
        PropertyValue metadataValues;
        if (rest != null && rest.length > 0) {
            metadataValues = new PropertyValue(value, rest);
        } else {
            metadataValues = new PropertyValue(value);
        }
        return new MetadataEntry(new PropertyValue(I18nUtil.getMessage("metadata." + field)), metadataValues);
    }
}
