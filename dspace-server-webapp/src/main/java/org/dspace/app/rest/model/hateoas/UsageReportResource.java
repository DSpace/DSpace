/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * The Resource representation of a {@link UsageReportRest} object
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
@RelNameDSpaceResource(UsageReportRest.NAME)
public class UsageReportResource extends DSpaceResource<UsageReportRest> {
    public UsageReportResource(UsageReportRest content, Utils utils) {
        super(content, utils);
    }
}
