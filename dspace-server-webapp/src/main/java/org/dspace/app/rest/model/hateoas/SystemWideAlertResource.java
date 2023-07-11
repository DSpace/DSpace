/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.alerts.SystemWideAlert;
import org.dspace.app.rest.model.SystemWideAlertRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * The Resource representation of a {@link SystemWideAlert} object
 */
@RelNameDSpaceResource(SystemWideAlertRest.NAME)
public class SystemWideAlertResource extends DSpaceResource<SystemWideAlertRest> {
    public SystemWideAlertResource(SystemWideAlertRest content, Utils utils) {
        super(content, utils);
    }
}
