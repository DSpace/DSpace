/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * Relationship HAL Resource. This resource adds the data from the REST object together with embedded objects
 * and a set of links if applicable
 */
@RelNameDSpaceResource(RelationshipRest.NAME)
public class RelationshipResource extends DSpaceResource<RelationshipRest> {
    public RelationshipResource(RelationshipRest data, Utils utils) {
        super(data, utils);

    }
}
