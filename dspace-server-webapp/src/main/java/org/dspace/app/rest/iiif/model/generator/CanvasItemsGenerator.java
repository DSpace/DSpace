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

import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the current Presentation API version 2.1.1 domain model's Sequence class.
 *
 * In Presentation API version 2.1.1, each Manifest includes a single Sequence that defines
 * the order of the views of the object.
 *
 * Sequence is removed with Presentation API version 3.0. Canvases are added to the Manifest
 * items property instead.
 */
@Component
@RequestScope
public class CanvasItemsGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

    private String identifier;
    private OtherContent rendering;
    private final List<Canvas> canvas = new ArrayList<>();

    @Autowired
    org.dspace.app.rest.iiif.model.generator.BehaviorGenerator viewingHintFascade;

    /**
     * Mandatory. The domain model requires a URI identifier for the sequence.
     * @param identifier string for the URI
     */
    public void setIdentifier(String identifier) {

        this.identifier = identifier;
    }

    /**
     * A link to an external resource intended for display or download by a human user.
     * This is typically going to be a PDF file.
     * @param otherContent wrapper for OtherContent
     */
    public void addRendering(org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator otherContent) {

        this.rendering = (OtherContent) otherContent.getResource();
    }

    /**
     * Add a Canvas to the sequence.
     * @param canvas wrapper for Canvas
     */
    public void addCanvas(org.dspace.app.rest.iiif.model.generator.CanvasGenerator canvas) {
        this.canvas.add((Canvas) canvas.getResource());
    }

    @Override
    public Resource<Sequence> getResource() {
        Sequence items = new Sequence(identifier);
        if (rendering != null) {
            items.addRendering(rendering);
        }
        items.setCanvases(canvas);
        return items;
    }
}
