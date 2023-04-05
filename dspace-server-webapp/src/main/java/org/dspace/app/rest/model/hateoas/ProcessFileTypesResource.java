/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ProcessFileTypesRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * Resource object for {@link ProcessFileTypesRest}
 */
@RelNameDSpaceResource(ProcessFileTypesRest.NAME)
public class ProcessFileTypesResource extends HALResource<ProcessFileTypesRest> {

    public ProcessFileTypesResource(ProcessFileTypesRest content) {
        super(content);
    }
}
