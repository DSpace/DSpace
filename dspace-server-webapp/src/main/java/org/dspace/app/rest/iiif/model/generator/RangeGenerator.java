/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;

/**
 * This generator wraps the domain model for IIIF {@code ranges}.
 *
 * In Presentation API version 2.1.1, adding a range to the manifest allows the client to display a structured
 * hierarchy to enable the user to navigate within the object without merely stepping through the current sequence.
 * The rationale for separating ranges from sequences is that there is likely to be overlap between different ranges,
 * such as the physical structure of a book compared to the textual structure of the work.
 */
public class RangeGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

    private String identifier;
    private String label;
    private final List<Canvas> canvasList = new ArrayList<>();

    /**
     * Sets mandatory range identifier.
     * @param identifier range identifier
     */
    public RangeGenerator setIdentifier(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid range identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
        return this;
    }

    /**
     * Sets range label.
     * @param label range label
     */
    public RangeGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Adds canvas to range canvas list.
     * @param canvas list of canvas generators
     */
    public RangeGenerator addCanvas(CanvasGenerator canvas) {
        canvasList.add((Canvas) canvas.generate());
        return this;
    }

    @Override
    public Resource<Range> generate() {
        if (identifier == null) {
            throw new RuntimeException("Missing identifier.  Cannot create range.");
        }
        Range range;
        if (label == null) {
            range = new Range(identifier);
        } else {
            range = new Range(identifier, label);
        }
        for (Canvas canvas : canvasList) {
            range.addCanvas(canvas);
        }
        return range;
    }
}
