/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CrisLayoutMetadataComponentRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * CrisLayoutMetadataComponent Rest HAL Resource.
 * The HAL Resource wraps the REST Resource adding support for the links and embedded resources
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@RelNameDSpaceResource(CrisLayoutMetadataComponentRest.NAME)
public class CrisLayoutMetadataComponentResource extends DSpaceResource<CrisLayoutMetadataComponentRest> {

    /**
     * @param data
     * @param utils
     */
    public CrisLayoutMetadataComponentResource(CrisLayoutMetadataComponentRest data, Utils utils) {
        super(data, utils);
    }

}
