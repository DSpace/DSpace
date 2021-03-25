/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.PropertyValue;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Supports single value PropertyValues.
 */
@Component
@Scope("prototype")
public class PropertyValueGenerator implements IIIFValue {

    private PropertyValue propertyValue;

    public void setPropertyValue(String label) {
        propertyValue = new PropertyValue(label);
    }

    public void setPropertyValue(String val1, String val2) {
        propertyValue = new PropertyValue(val1, val2);
    }

    @Override
    public PropertyValue getValue() {
        return propertyValue;
    }
}
