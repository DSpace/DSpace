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
import de.digitalcollections.iiif.model.search.SearchResult;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps a domain model for a {@code SearchResult}.
 *
 * <p>Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 */
@RequestScope
@Component
public class SearchResultGenerator implements IIIFResource {

    private String identifier;
    private final List<Annotation> annotations = new ArrayList<>();

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void addResource(AnnotationGenerator annotation) {
        annotations.add((Annotation) annotation.generateResource());
    }

    @Override
    public Resource<Annotation> generateResource() {
        SearchResult searchResult = new SearchResult(identifier);
        searchResult.setResources(annotations);
        return searchResult;
    }
}
