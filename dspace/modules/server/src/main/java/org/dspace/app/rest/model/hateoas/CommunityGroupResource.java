/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.CommunityGroupRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * CommunityGroup Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author Mohamed Abdul Rasheed (mohideen at umd.edu)
 *
 */
@RelNameDSpaceResource(CommunityGroupRest.NAME)
public class CommunityGroupResource extends DSpaceResource<CommunityGroupRest> {
    public CommunityGroupResource(CommunityGroupRest communityGroup, Utils utils) {
        super(communityGroup, utils);
    }
}
