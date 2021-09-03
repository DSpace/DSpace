/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.OtherContent;
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

    private OtherContent otherContent;

    public ExternalLinksGenerator(String identifier) {
        otherContent = new OtherContent(identifier);
    }

    /**
     * Sets the optional format.
     * @param format
     */
    public void setFormat(String format) {
        otherContent.setFormat(format);
    }

    /**
     * Sets the optional label.
     * @param label
     */
    public void setLabel(String label) {
        otherContent.setLabel(new PropertyValueGenerator().getPropertyValue(label).getValue());
    }

    /**
     * Sets the optional type.
     * @param type
     */
    public void setType(String type) {
        otherContent.setType(type);
    }

    @Override
    public Resource<OtherContent> getResource() {
        return otherContent;
    }

}
