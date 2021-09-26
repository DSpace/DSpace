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

import javax.validation.constraints.NotNull;

import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps the domain model for the {@code AnnotationList}. There should be a single instance of
 * this object per annotation list request. The {@code @RequestScope} provides a single instance created
 * and available during complete lifecycle of the HTTP request.
 * <p>
 * The model represents an ordered list of annotations.</p>
 */
@RequestScope
@Component
public class AnnotationListGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

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
        this.annotations.add((Annotation) annotation.generate());
    }

    @Override
    public Resource<Annotation> generate() {
        if (identifier == null) {
            throw new RuntimeException("Missing the required identifier for the annotation list.");
        }
        AnnotationList annotationList = new AnnotationList(identifier);
        annotationList.setResources(annotations);
        return annotationList;
    }
}
