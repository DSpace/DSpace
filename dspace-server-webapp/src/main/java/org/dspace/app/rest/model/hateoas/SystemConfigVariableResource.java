/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.SystemConfigVariableRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * HAL Resource wrapper for {@link SystemConfigVariableRest}.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@RelNameDSpaceResource(SystemConfigVariableRest.NAME)
public class SystemConfigVariableResource extends DSpaceResource<SystemConfigVariableRest> {

    /**
     * Constructs a HAL resource wrapping the given system config variable REST object.
     *
     * @param content the system config variable REST object to wrap
     * @param utils   the REST utilities used to build resource links
     */
    public SystemConfigVariableResource(SystemConfigVariableRest content, Utils utils) {
        super(content, utils);
    }
}
