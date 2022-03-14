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

import de.digitalcollections.iiif.model.Motivation;
import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;


/**
 * Generator for an {@code annotation} model. Annotations associate content resources and commentary with a canvas.
 * This is used for the {@code seeAlso} annotation and Search response.
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


    public AnnotationGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid annotation identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    public AnnotationGenerator(@NotNull String identifier, @NotNull Motivation motivation) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid annotation identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
        this.motivation = motivation;
    }

    /**
     * Sets the motivation field. Required.
     * @param motivation the motivation
     * @return
     */
    public AnnotationGenerator setMotivation(@NotNull Motivation motivation) {
        this.motivation = motivation;
        return this;
    }

    /**
     * Sets the canvas that is associated with this annotation.
     * @param canvas
     * @return
     */
    public AnnotationGenerator setOnCanvas(CanvasGenerator canvas) {
        this.canvasGenerator = canvas;
        return this;
    }

    /**
     * Sets a text resource for this annotation.
     * @param contentAsText
     * @return
     */
    public AnnotationGenerator setResource(ContentAsTextGenerator contentAsText) {
        this.contentAsTextGenerator = contentAsText;
        return this;
    }

    /**
     * Sets an external link for this annotation.
     * @param otherContent external link generator
     * @return
     */
    public AnnotationGenerator setResource(ExternalLinksGenerator otherContent) {
        this.externalLinksGenerator = otherContent;
        return this;
    }

    /**
     * Set the within property for this annotation. This property
     * is a list of manifests. The property is renamed to partOf in v3
     * <p>Used by search result annotations.</p>
     * @param within
     * @return
     */
    public AnnotationGenerator setWithin(List<ManifestGenerator> within) {
        for (ManifestGenerator manifest : within) {
            this.manifests.add(manifest.generateResource());
        }
        return this;
    }

    @Override
    public Resource<Annotation> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("Annotations require an identifier.");
        }
        Annotation annotation;
        if (motivation != null) {
            annotation = new Annotation(identifier, motivation);
        } else {
            annotation = new Annotation(identifier);
        }
        annotation.setWithin(manifests);
        // These optional annotation fields vary with the context.
        if (canvasGenerator != null) {
            annotation.setOn(canvasGenerator.generateResource());
        }
        if (externalLinksGenerator != null) {
            annotation.setResource(externalLinksGenerator.generateResource());
        }
        if (contentAsTextGenerator != null) {
            annotation.setResource(contentAsTextGenerator.generateResource());
        }
        return annotation;
    }
}
