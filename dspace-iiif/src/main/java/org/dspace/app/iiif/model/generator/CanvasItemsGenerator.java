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

import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the domain model for a Presentation API 2.1.1 {@code Sequence}. The IIIF sequence
 * conveys the ordering of the views of the object.
 *
 * <p>Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 *
 * <p>Sequence is removed with Presentation API version 3.0. Canvases are added to the Manifest items property instead.
 * </p>
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class CanvasItemsGenerator implements IIIFResource {

    private String identifier;
    private final List<Canvas> canvas = new ArrayList<>();

    /**
     * Sets the required identifier property.
     * @param identifier URI string
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }


    /**
     * Adds a single {@code Canvas} to the sequence.
     * @param canvas generator for canvas
     */
    public String addCanvas(CanvasGenerator canvas) {
        Canvas resource = (Canvas) canvas.generateResource();
        this.canvas.add(resource);
        return resource.getIdentifier().toString();
    }

    @Override
    public Resource<Sequence> generateResource() {
        Sequence items = new Sequence(identifier);
        items.setCanvases(canvas);
        return items;
    }
}
