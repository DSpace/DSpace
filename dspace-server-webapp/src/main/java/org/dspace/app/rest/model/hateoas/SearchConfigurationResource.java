/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchConfigurationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This class serves as a wrapper class to wrap the SearchConfigurationRest into a HAL resource
 */
@RelNameDSpaceResource(SearchConfigurationRest.NAME)
public class SearchConfigurationResource extends HALResource<SearchConfigurationRest> {

    public SearchConfigurationResource(SearchConfigurationRest searchConfigurationRest) {
        super(searchConfigurationRest);
    }

}
