/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import java.sql.SQLException;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * The HALResource object for the {@link VersionRest} object
 */
@RelNameDSpaceResource(VersionRest.NAME)
public class VersionResource extends DSpaceResource<VersionRest> {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(VersionResource.class);

    public VersionResource(VersionRest data, Utils utils) {
        super(data, utils);
        if (isAdmin() || DSpaceServicesFactory.getInstance().getConfigurationService()
                                              .getBooleanProperty("versioning.item.history.include.submitter")) {
            add(utils.linkToSubResource(data, "eperson"));
        }
    }

    private boolean isAdmin() {
        try {
            return AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(
                ContextUtil.obtainContext(
                    DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest()
                                         .getHttpServletRequest()));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
