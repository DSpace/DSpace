/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchFacetValueRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class' purpose is to create a container for the information, links and embeds for the facet values on various
 * endpoints
 */
@RelNameDSpaceResource(SearchFacetValueRest.NAME)
public class SearchFacetValueResource extends DSpaceResource<SearchFacetValueRest> {
    public SearchFacetValueResource(final SearchFacetValueRest data, Utils utils) {
        super(data, utils);
    }
}
