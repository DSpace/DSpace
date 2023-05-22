/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SupervisionOrderRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * SupervisionOrder Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
@RelNameDSpaceResource(SupervisionOrderRest.NAME)
public class SupervisionOrderResource extends DSpaceResource<SupervisionOrderRest> {
    public SupervisionOrderResource(SupervisionOrderRest data, Utils utils) {
        super(data, utils);
    }
}
