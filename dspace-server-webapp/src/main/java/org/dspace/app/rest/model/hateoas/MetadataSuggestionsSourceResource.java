/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MetadataSuggestionsSourceRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * The Resource object for {@link MetadataSuggestionsSourceRest} object
 */
@RelNameDSpaceResource(MetadataSuggestionsSourceRest.NAME)
public class MetadataSuggestionsSourceResource extends HALResource<MetadataSuggestionsSourceRest> {
    public MetadataSuggestionsSourceResource(MetadataSuggestionsSourceRest content) {
        super(content);
    }
}
