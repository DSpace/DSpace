/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.HarvesterMetadataRest;
import org.dspace.app.rest.utils.Utils;

/**
 * Harvester metadata rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links.
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class HarvesterMetadataResource extends DSpaceResource<HarvesterMetadataRest> {

    public HarvesterMetadataResource(HarvesterMetadataRest data, Utils utils, String... rels) {
        super(data, utils, rels);

    }
}
