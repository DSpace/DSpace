/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.ProcessOutputRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 * This is the Resource class for the {@link ProcessOutputRest}
 */
@RelNameDSpaceResource(ProcessOutputRest.NAME)
public class ProcessOutputResource extends HALResource<ProcessOutputRest> {
    public ProcessOutputResource(ProcessOutputRest content) {
        super(content);
    }
}
