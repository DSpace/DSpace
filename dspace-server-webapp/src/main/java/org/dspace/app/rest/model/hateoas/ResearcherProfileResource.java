/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class serves as a wrapper class to wrap the SearchConfigurationRest into
 * a HAL resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RelNameDSpaceResource(ResearcherProfileRest.NAME)
public class ResearcherProfileResource extends DSpaceResource<ResearcherProfileRest> {

    public ResearcherProfileResource(ResearcherProfileRest data, Utils utils) {
        super(data, utils);
    }


}
