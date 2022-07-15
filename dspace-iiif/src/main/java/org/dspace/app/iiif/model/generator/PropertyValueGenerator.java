/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.PropertyValue;

/**
 * This class wraps the domain model property value annotation. The property is the type for
 * strings that are intended to be displayed to the user.
 */
public class PropertyValueGenerator implements IIIFValue {

    private PropertyValue propertyValue;

    public PropertyValueGenerator getPropertyValue(String val1, String val2) {
        propertyValue = new PropertyValue(val1, val2);
        return this;
    }

    public PropertyValueGenerator getPropertyValue(String val1) {
        propertyValue = new PropertyValue(val1);
        return this;
    }

    @Override
    public PropertyValue generateValue() {
        return propertyValue;
    }
}
