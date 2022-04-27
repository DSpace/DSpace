/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class serves as a wrapper class to wrap the OrcidQueueRest into a HAL
 * resource.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RelNameDSpaceResource(OrcidQueueRest.NAME)
public class OrcidQueueResource extends DSpaceResource<OrcidQueueRest> {

    public OrcidQueueResource(OrcidQueueRest data, Utils utils) {
        super(data, utils);
    }


}
