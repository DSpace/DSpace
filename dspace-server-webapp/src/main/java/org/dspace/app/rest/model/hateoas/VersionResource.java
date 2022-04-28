/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * The HALResource object for the {@link VersionRest} object
 */
@RelNameDSpaceResource(VersionRest.NAME)
public class VersionResource extends DSpaceResource<VersionRest> {
    public VersionResource(VersionRest data, Utils utils) {
        super(data, utils);
    }


}
