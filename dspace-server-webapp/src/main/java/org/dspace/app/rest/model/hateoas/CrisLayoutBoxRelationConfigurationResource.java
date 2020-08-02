/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CrisLayoutBoxRelationConfigurationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@RelNameDSpaceResource(CrisLayoutBoxRelationConfigurationRest.NAME)
public class CrisLayoutBoxRelationConfigurationResource
    extends DSpaceResource<CrisLayoutBoxRelationConfigurationRest> {

    /**
     * @param data
     * @param utils
     */
    public CrisLayoutBoxRelationConfigurationResource(CrisLayoutBoxRelationConfigurationRest data, Utils utils) {
        super(data, utils);
    }

}
