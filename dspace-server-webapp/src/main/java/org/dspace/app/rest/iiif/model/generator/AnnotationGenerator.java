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
import de.digitalcollections.iiif.model.sharedcanvas.Resource;


/**
 * Annotations associate content resources and commentary with a canvas.
 * This is used for the otherContent AnnotationList and Search response.
 */
public class AnnotationGenerator implements IIIFResource {

    public static final String TYPE = "sc:AnnotationList";
    public static final Motivation PAINTING = new Motivation("sc:painting");
    public static final Motivation COMMENTING = new Motivation("oa:commenting");
    public static final Motivation LINKING = new Motivation("oa:linking");

    private Motivation motivation;
    private String identifier;
    private CanvasGenerator canvasGenerator;
    private ContentAsTextGenerator contentAsTextGenerator;
    private ExternalLinksGenerator externalLinksGenerator;
    List<Resource> manifests = new ArrayList<>();


    /**
     * Set the annotation identifier. Required.
     * @param identifier
     * @return
     */
    public AnnotationGenerator setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Sets the annotation motivtion. Required.
     * @param motivation
     * @return
     */
    public AnnotationGenerator setMotivation(Motivation motivation) {
        this.motivation = motivation;
        return this;
    }

    /**
     * Set the canvas for this annotation.
     * @param canvas
     * @return
     */
    public AnnotationGenerator setOnCanvas(CanvasGenerator canvas) {
        this.canvasGenerator = canvas;
        return this;
    }

    /**
     * Set a text resource for this annotation.
     * @param contentAsText
     * @return
     */
    public AnnotationGenerator setResource(ContentAsTextGenerator contentAsText) {
        this.contentAsTextGenerator = contentAsText;
        return this;
    }

    /**
     * Set the external link for this annotation.
     * @param otherContent
     * @return
     */
    public AnnotationGenerator setResource(ExternalLinksGenerator otherContent) {
        this.externalLinksGenerator = otherContent;
        return this;
    }

    /**
     * Set the within property for this annotation. This property
     * is a list of manifests. The property is renamed to partOf in v3
     * @param within
     * @return
     */
    public AnnotationGenerator setWithin(List<ManifestGenerator> within) {
        for (ManifestGenerator manifest : within) {
            this.manifests.add(manifest.getResource());
        }
        return this;
    }

    @Override
    public Resource<Annotation> getResource() {
        if (identifier == null || motivation == null) {
            throw new RuntimeException("Annotations require both an identifier and a motivation");
        }
        Annotation annotation = new Annotation(identifier, motivation);
        annotation.setWithin(manifests);
        // These optional annotation fields vary with the context.
        if (canvasGenerator != null) {
            annotation.setOn(canvasGenerator.getResource());
        }
        if (externalLinksGenerator != null) {
            annotation.setResource(externalLinksGenerator.getResource());
        }
        if (contentAsTextGenerator != null) {
            annotation.setResource(contentAsTextGenerator.getResource());
        }
        return annotation;
    }
}
