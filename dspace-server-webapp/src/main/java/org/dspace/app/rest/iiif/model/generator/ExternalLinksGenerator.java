/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Facade for the IIIF Presentation API version 2.1.1 "OtherContent" domain model class.
 *
 * This is the type for Content resources such as images or texts that are associated with a canvas.
 * Used in the "related", "renderings" and "otherContent" fields of IIIF resources.
 *
 * IIIF Presentation API version 3.0 removes the otherContent property and uses annotations
 * and items instead.
 */
@Component
public class ExternalLinksGenerator implements IIIFResource {

    @Autowired
    PropertyValueGenerator propertyValue;

    private String identifier;
    private String format;
    private PropertyValue label;
    private String type;

    /**
     * Sets the mandatory identifier.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the optional format.
     * @param format
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Sets the optional label.
     * @param label
     */
    public void setLabel(String label) {
        propertyValue.setPropertyValue(label);
        this.label = propertyValue.getValue();
    }

    /**
     * Sets the optional type.
     * @param type
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Resource<OtherContent> getResource() {
        OtherContent otherContent;
        if (format != null) {
            otherContent = new OtherContent(identifier, format);
        } else {
            otherContent = new OtherContent(identifier);
        }
        if (label != null) {
            otherContent.setLabel(label);
        }
        if (type != null) {
            otherContent.setType(type);
        }

        // Reset facade properties after creating the resource.
        identifier = null;
        format = null;
        label = null;
        type = null;

        return otherContent;
    }

}
