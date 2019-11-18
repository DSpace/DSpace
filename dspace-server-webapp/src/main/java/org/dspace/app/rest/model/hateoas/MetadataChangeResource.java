/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MetadataChangeRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This HalResource acts as a resource class for the {@link MetadataChangeRest} object
 */
@RelNameDSpaceResource(MetadataChangeRest.NAME)
public class MetadataChangeResource extends HALResource<MetadataChangeRest> {
    /**
     * Default constructor for the resource
     * @param content   The relevant REST object
     */
    public MetadataChangeResource(MetadataChangeRest content) {
        super(content);
    }
}
