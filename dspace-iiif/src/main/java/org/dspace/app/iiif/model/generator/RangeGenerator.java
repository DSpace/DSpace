/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

import de.digitalcollections.iiif.model.enums.ViewingHint;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.dspace.app.iiif.service.RangeService;

/**
 * This generator wraps the domain model for IIIF {@code ranges}.
 *
 * In Presentation API version 2.1.1, adding a range to the manifest allows the client to display a structured
 * hierarchy to enable the user to navigate within the object without merely stepping through the current sequence.
 *
 * This is used to populate the "structures" element of the Manifest. The structure is derived from the iiif.toc
 * metadata and the ordered sequence of bitstreams (canvases)
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class RangeGenerator implements IIIFResource {

    private String identifier;
    private String label;
    private final List<ViewingHint> viewingHint = new ArrayList<>();
    private final List<Canvas> canvasList = new ArrayList<>();
    private final List<Range> rangesList = new ArrayList<>();
    private final RangeService rangeService;

    /**
     * The {@code RangeService} is used for defining hierarchical sub ranges.
     * @param rangeService range service
     */
    public RangeGenerator(RangeService rangeService) {
        this.rangeService = rangeService;
    }

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

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the optional range label.
     * @param label range label
     */
    public RangeGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    public RangeGenerator addViewingHint(String hint) {
        viewingHint.add(new BehaviorGenerator().setType(hint).generateValue());
        return this;
    }

    /**
     * Adds canvas to range canvas list.
     * @param canvas list of canvas generators
     */
    public RangeGenerator addCanvas(CanvasGenerator canvas) {
        canvasList.add((Canvas) canvas.generateResource());
        return this;
    }

    /**
     * Sets the range identifier and adds a sub range to the ranges list.
     * @param range range generator
     */
    public void addSubRange(RangeGenerator range) {
        range.setIdentifier(identifier + "-" + rangesList.size());
        RangeGenerator rangeReference = rangeService.getRangeReference(range);
        rangesList.add((Range) rangeReference.generateResource());
    }

    @Override
    public Resource<Range> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("The Range resource requires an identifier.");
        }
        Range range;
        if (label != null) {
            range = new Range(identifier, label);
        } else {
            range = new Range(identifier);
        }
        if (viewingHint.size() > 0) {
            range.setViewingHints(viewingHint);
        }
        for (Canvas canvas : canvasList) {
            range.addCanvas(canvas);
        }
        for (Range rangeResource : rangesList) {
            range.addRange(rangeResource);
        }
        return range;
    }
}
