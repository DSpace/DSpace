/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CrisLayoutMetricsConfigurationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * CrisLayoutMetricsConfigurationRest Rest HAL Resource.
 * The HAL Resource wraps the REST Resource adding support for the links and embedded resources
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
@RelNameDSpaceResource(CrisLayoutMetricsConfigurationRest.NAME)
public class CrisLayoutMetricsConfigurationResource extends DSpaceResource<CrisLayoutMetricsConfigurationRest> {

    /**
     * @param data
     * @param utils
     */
    public CrisLayoutMetricsConfigurationResource(CrisLayoutMetricsConfigurationRest data, Utils utils) {
        super(data, utils);
    }

}
