/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.LoginStatisticsRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Login statistics HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RelNameDSpaceResource(LoginStatisticsRest.NAME)
public class LoginStatisticsResource extends DSpaceResource<LoginStatisticsRest> {

    public LoginStatisticsResource(LoginStatisticsRest item, Utils utils) {
        super(item, utils);
    }

}
