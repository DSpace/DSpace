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

import de.digitalcollections.iiif.model.Motivation;
import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the IIIF Presentation API version 2.2.1 domain model. Annotations associate
 * content resources and commentary with a canvas.
 *
 * This facade is provided for serializing AnnotationList and Search query responses.
 */
@Component
@RequestScope
public class AnnotationGenerator implements IIIFResource {

    private String identifier;
    private Canvas onCanvas;
    private Motivation motivation;
    private ContentAsTextGenerator contentAsText;
    private ExternalLinksGenerator otherContent;
    // Renamed to "partOf" in IIIF v 3.0
    private List<ManifestGenerator> within;

    public static final Motivation PAINTING = new Motivation("sc:painting");
    public static final Motivation COMMENTING = new Motivation("oa:commenting");
    public static final Motivation LINKING = new Motivation("oa:linking");

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setMotivation(Motivation motivation) {
        this.motivation = motivation;
    }

    public void setOnCanvas(org.dspace.app.rest.iiif.model.generator.CanvasGenerator canvas) {
        this.onCanvas = (Canvas) canvas.getResource();
    }

    public void setResource(org.dspace.app.rest.iiif.model.generator.ContentAsTextGenerator contentAsText) {
        this.contentAsText = contentAsText;
    }

    public void setResource(org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator otherContent) {
        this.otherContent = otherContent;
    }

    public void setWithin(List<ManifestGenerator> within) {
        this.within = within;
    }

    @Override
    public Resource<Annotation> getResource() {
        Annotation annotation = new Annotation(identifier, motivation);
        if (contentAsText != null) {
            annotation.setResource(contentAsText.getResource());
        }
        if (otherContent != null) {
            annotation.setResource(otherContent.getResource());
        }
        if (onCanvas != null) {
            annotation.setOn(onCanvas);
        }
        if (within != null) {
            List<Resource> manifests = new ArrayList<>();
            for (ManifestGenerator manifest : within) {
                manifests.add(manifest.getResource());
            }
            annotation.setWithin(manifests);
        }
        within = null;
        identifier = null;
        otherContent = null;
        onCanvas = null;
        return annotation;
    }
}
