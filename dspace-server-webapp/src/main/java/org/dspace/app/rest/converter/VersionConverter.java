/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.versioning.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter that takes care of the conversion between {@link Version} and {@link VersionRest}
 */
@Component
public class VersionConverter implements DSpaceConverter<Version, VersionRest> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RequestService requestService;

    @Override
    public VersionRest convert(Version modelObject, Projection projection) {
        VersionRest versionRest = new VersionRest();
        versionRest.setCreated(modelObject.getVersionDate());
        versionRest.setId(modelObject.getID());
        versionRest.setSummary(modelObject.getSummary());
        setSubmitterName(modelObject, versionRest);
        versionRest.setVersion(modelObject.getVersionNumber());
        versionRest.setProjection(projection);
        return versionRest;
    }

    private void setSubmitterName(Version modelObject, VersionRest versionRest) {
        Context context = null;
        Request currentRequest = requestService.getCurrentRequest();
        if (currentRequest != null) {
            context = ContextUtil.obtainContext(currentRequest.getHttpServletRequest());
        }
        try {
            if ((context != null && authorizeService.isAdmin(context)) || configurationService
                .getBooleanProperty("versioning.item.history.include.submitter")) {
                EPerson submitter = modelObject.getEPerson();
                if (submitter != null) {
                    versionRest.setSubmitterName(submitter.getFullName());
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Class<Version> getModelClass() {
        return Version.class;
    }
}
