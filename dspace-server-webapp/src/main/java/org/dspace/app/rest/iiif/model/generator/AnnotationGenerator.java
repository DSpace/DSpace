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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Annotations associate content resources and commentary with a canvas.
 * This is used for the otherContent AnnotationList and Search response.
 */
@Component
@Scope("prototype")
public class AnnotationGenerator implements IIIFResource {

    public static final String TYPE = "sc:AnnotationList";
    public static final Motivation PAINTING = new Motivation("sc:painting");
    public static final Motivation COMMENTING = new Motivation("oa:commenting");
    public static final Motivation LINKING = new Motivation("oa:linking");

    private Annotation annotation;

    public AnnotationGenerator(String identifier, Motivation motivation) {
        annotation = new Annotation(identifier, motivation);
    }

    public void setOnCanvas(CanvasGenerator canvas) {
        annotation.setOn(canvas.getResource());
    }

    public void setResource(org.dspace.app.rest.iiif.model.generator.ContentAsTextGenerator contentAsText) {
        annotation.setResource(contentAsText.getResource());
    }

    public void setResource(org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator otherContent) {
        annotation.setResource(otherContent.getResource());
    }

    public void setWithin(List<ManifestGenerator> within) {
        List<Resource> manifests = new ArrayList<>();
        for (ManifestGenerator manifest : within) {
            manifests.add(manifest.getResource());
        }
        // property renamed to partOf in v3
        annotation.setWithin(manifests);
    }

    @Override
    public Resource<Annotation> getResource() {
        return annotation;
    }
}
