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

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * In Presentation API version 2.1.1, adding a range to the manifest allows the client to display a structured
 * hierarchy to enable the user to navigate within the object without merely stepping through the current sequence.
 * The rationale for separating ranges from sequences is that there is likely to be overlap between different ranges,
 * such as the physical structure of a book compared to the textual structure of the work.
 *
 * This is used to populate the "structures" element of the Manifest. (The REST API service looks to the "info.json"
 * file for ranges.)
 */
@Component
@Scope("prototype")
public class RangeGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

    private String identifier;
    private String label;
    private final List<Canvas> canvasList = new ArrayList<>();

    /**
     * Sets mandatory range identifier.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets mandatory range label.
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Adds canvas to Range canvas list.
     * @param canvas
     */
    public void addCanvas(org.dspace.app.rest.iiif.model.generator.CanvasGenerator canvas) {
        canvasList.add((Canvas) canvas.getResource());
    }

    @Override
    public Resource<Range> getResource() {
        Range range = new Range(identifier, label);
        for (Canvas canvas : canvasList) {
            range.addCanvas(canvas);
        }
        // Reset properties after each use
        identifier = null;
        canvasList.clear();
        label = null;

        return range;
    }
}
