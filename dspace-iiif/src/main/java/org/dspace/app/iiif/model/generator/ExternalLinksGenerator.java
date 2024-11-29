/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import jakarta.validation.constraints.NotNull;

/**
 * This generator wraps the other content domain model.
 *
 * This is the type for related content resources. Used in the "related", "renderings" and
 * "seeAlso" fields of IIIF resources.
 */
public class ExternalLinksGenerator implements IIIFResource {

    private final String identifier;
    private String format;
    private String label;
    private String type;

    public ExternalLinksGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Mandatory external links identifier cannot be an empty string");
        }
        this.identifier = identifier;
    }

    /**
     * Sets the optional format value.
     * @param format the mimetype
     */
    public ExternalLinksGenerator setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the optional label.
     * @param label annotation label
     */
    public ExternalLinksGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the optional type.
     * @param type the annotation type
     */
    public ExternalLinksGenerator setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public Resource<OtherContent> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("External links annotation requires an identifier");
        }
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
