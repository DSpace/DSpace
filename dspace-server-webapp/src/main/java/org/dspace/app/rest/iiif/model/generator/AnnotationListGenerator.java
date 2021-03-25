/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.sharedcanvas.AnnotationList;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An ordered list of annotations. Annotation Lists are separate resources that
 * should be dereferenced when encountered.
 *
 * This class is used when retrieving an AnnotationList referenced in the Manifest.
 */
@Component
@Scope("prototype")
public class AnnotationListGenerator implements org.dspace.app.rest.iiif.model.generator.IIIFResource {

    private String identifier;
    private Annotation annotation;

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void addResource(org.dspace.app.rest.iiif.model.generator.AnnotationGenerator annotation) {
        this.annotation = (Annotation) annotation.getResource();
    }

    @Override
    public Resource<Annotation> getResource() {
        AnnotationList annotations = new AnnotationList(identifier);
        annotations.addResource(annotation);
        return annotations;
    }
}
