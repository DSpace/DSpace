/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.enums.ViewingHint;

/**
 * API 2.1.1 ViewingHint is a hint to the client that suggests the appropriate method of
 * displaying the resource.
 *
 * With IIIF Presentation API 3.0 the viewingHint property is renamed to "behavior".
 */
public class BehaviorGenerator implements IIIFValue {

    private String type;

    public BehaviorGenerator setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public ViewingHint generateValue() {
        if (type == null) {
            throw new RuntimeException("Type must be provided for viewing hint.");
        }
        return new ViewingHint(type);
    }
}
