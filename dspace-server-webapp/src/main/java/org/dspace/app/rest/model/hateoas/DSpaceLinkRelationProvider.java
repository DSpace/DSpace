/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.server.core.EvoInflectorLinkRelationProvider;

/**
 * A DSpace Link Relation Provider that use the RelNameDSpaceResource annotation to use the
 * right names for the embedded collection when a DSpaceResource is requested
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class DSpaceLinkRelationProvider extends EvoInflectorLinkRelationProvider {

    @Override
    public LinkRelation getItemResourceRelFor(Class<?> type) {
        RelNameDSpaceResource nameAnnotation = AnnotationUtils.findAnnotation(type, RelNameDSpaceResource.class);
        if (nameAnnotation != null) {
            return LinkRelation.of(nameAnnotation.value());
        }
        return super.getItemResourceRelFor(type);
    }

}
