/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.OrcidHistoryRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * This class serves as a wrapper class to wrap the OrcidHistoryRest into a HAL
 * resource.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
@RelNameDSpaceResource(OrcidHistoryRest.NAME)
public class OrcidHistoryResource extends DSpaceResource<OrcidHistoryRest> {

    public OrcidHistoryResource(OrcidHistoryRest data, Utils utils) {
        super(data, utils);
    }

}
