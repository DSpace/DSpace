/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SearchConfigurationRest;

/**
 * Created by raf on 22/09/2017.
 */
@RelNameDSpaceResource(SearchConfigurationRest.NAME)
public class SearchConfigurationResource extends HALResource<SearchConfigurationRest> {

    public SearchConfigurationResource(SearchConfigurationRest searchConfigurationRest){
        super(searchConfigurationRest);
    }

}
