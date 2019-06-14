/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchSupportRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This class' purpose is to wrap the SearchSupportRest into a HAL resource
 */
@RelNameDSpaceResource(SearchSupportRest.NAME)
public class SearchSupportResource extends HALResource<SearchSupportRest> {

    public SearchSupportResource(SearchSupportRest searchSupportRest) {
        super(searchSupportRest);
    }

}