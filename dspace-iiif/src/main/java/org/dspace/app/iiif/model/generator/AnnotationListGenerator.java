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

import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the domain model for the {@code AnnotationList}.
 *
 * <p>Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 *
 * <p>The model represents an ordered list of annotations.</p>
 */
@RequestScope
@Component
public class AnnotationListGenerator implements IIIFResource {

    private String identifier;
    private List<Annotation> annotations = new ArrayList<>();

    /**
     * Sets the required annotation identifier.
     * @param identifier the annotation identifier
     */
    public void setIdentifier(@NotNull String identifier) {

        this.identifier = identifier;
    }

    /**
     * Adds Annotation resource to the annotation list.
     * @param annotation an annotation generator
     */
    public void addResource(AnnotationGenerator annotation) {
        this.annotations.add((Annotation) annotation.generateResource());
    }

    @Override
    public Resource<Annotation> generateResource() {
        if (identifier == null) {
            throw new RuntimeException("Missing the required identifier for the annotation list.");
        }
        AnnotationList annotationList = new AnnotationList(identifier);
        annotationList.setResources(annotations);
        return annotationList;
    }
}
