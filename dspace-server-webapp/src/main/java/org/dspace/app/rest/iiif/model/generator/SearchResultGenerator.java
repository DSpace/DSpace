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

import de.digitalcollections.iiif.model.openannotation.Annotation;
import de.digitalcollections.iiif.model.search.SearchResult;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the AnnotationList that contains hits for a given search query.
 */
@Component
@RequestScope
public class SearchResultGenerator implements IIIFResource {

    private String identifier;
    private final List<Annotation> annotations = new ArrayList<>();

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void addResource(AnnotationGenerator annotation) {
        annotations.add((Annotation) annotation.getResource());
    }

    @Override
    public Resource<Annotation> getResource() {
        SearchResult searchResult = new SearchResult(identifier);
        searchResult.setResources(annotations);
        return searchResult;
    }
}
