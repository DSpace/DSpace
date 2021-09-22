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

/**
 * In Presentation API version 2.1.1, adding a range to the manifest allows the client to display a structured
 * hierarchy to enable the user to navigate within the object without merely stepping through the current sequence.
 * The rationale for separating ranges from sequences is that there is likely to be overlap between different ranges,
 * such as the physical structure of a book compared to the textual structure of the work.
 *
 * This is used to populate the "structures" element of the Manifest. The structure is derived from the iiif.toc
 * metadata and the ordered sequence of bitstreams (canvases)
 */
public class RangeGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

    private String identifier;
    private String label;
    private final List<Canvas> canvasList = new ArrayList<>();
    private final List<RangeGenerator> rangesList = new ArrayList<>();

    /**
     * Sets mandatory range identifier.
     * @param identifier range identifier
     */
    public RangeGenerator setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets mandatory range label.
     * @param label range label
     */
    public RangeGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Adds canvas to Range canvas list.
     * @param canvas list of canvas models
     */
    public RangeGenerator addCanvas(CanvasGenerator canvas) {
        canvasList.add((Canvas) canvas.getResource());
        return this;
    }

    @Override
    public Resource<Range> getResource() {
        Range range = new Range(identifier, label);
        for (Canvas canvas : canvasList) {
            range.addCanvas(canvas);
        }
        for (RangeGenerator rg : rangesList) {
            range.addRange((Range) rg.getResource());
        }
        return range;
    }

    public void addSubRange(RangeGenerator range) {
        range.setIdentifier(identifier + "-" + rangesList.size());
        rangesList.add(range);
    }
}
