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
import org.springframework.context.annotation.Scope;
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
@Scope("prototype")
public class ExternalLinksGenerator implements IIIFResource {

    private String identifier;
    private String format;
    private String label;
    private String type;

    /**
     * Sets the mandatory identifier.
     * @param identifier
     */
    public ExternalLinksGenerator setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Sets the optional format.
     * @param format
     */
    public ExternalLinksGenerator setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the optional label.
     * @param label
     */
    public ExternalLinksGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the optional type.
     * @param type
     */
    public ExternalLinksGenerator setType(String type) {
        this.type = type;
        return this;
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
            otherContent.setLabel(new PropertyValue(label));
        }
        if (type != null) {
            otherContent.setType(type);
        }

        return otherContent;
    }

}
