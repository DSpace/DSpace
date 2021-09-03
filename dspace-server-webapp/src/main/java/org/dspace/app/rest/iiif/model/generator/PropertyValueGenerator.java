/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.PropertyValue;

/**
 * Type for strings that are intended to be displayed to the user.
 *
 * <p>Is organized as a mapping of languages to one or more values. See
 * http://iiif.io/api/presentation/2.1/#language-of-property-values and
 * http://iiif.io/api/presentation/2.1/#html-markup-in-property-values for more information.
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
    public PropertyValue getValue() {
        return propertyValue;
    }
}
