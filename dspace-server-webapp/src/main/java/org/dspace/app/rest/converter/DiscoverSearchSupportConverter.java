/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.SearchSupportRest;
import org.springframework.stereotype.Component;

/**
 * This class' purpose is to create a SearchSupportRest object to return
 */
@Component
public class DiscoverSearchSupportConverter {

    public SearchSupportRest convert() {
        return new SearchSupportRest();
    }
}
