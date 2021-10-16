/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.PropertyValue;
import org.dspace.core.I18nUtil;

/**
 * Wraps the domain model metadata property.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class MetadataEntryGenerator implements IIIFValue {

    private String field;
    private String value;
    private String[] rest;

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
    public MetadataEntryGenerator setValue(String value, String... rest) {
        this.value = value;
        this.rest = rest;
        return this;
    }

    @Override
    public MetadataEntry generateValue() {
        PropertyValue metadataValues;
        if (rest != null && rest.length > 0) {
            metadataValues = new PropertyValue(value, rest);
        } else {
            metadataValues = new PropertyValue(value);
        }
        return new MetadataEntry(new PropertyValue(I18nUtil.getMessage("metadata." + field)), metadataValues);
    }
}
