/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class acts as a PermissionEvaluator to decide whether a given request to a Versioning endpoint is allowed to
 * pass through or not.
 */
@Component
public class VersionRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private RequestService requestService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConfigurationService configurationService;


    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {


        if (!StringUtils.equalsIgnoreCase(targetType, VersionRest.NAME) || Objects.isNull(targetId)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        try {
            int versionId = Integer.parseInt(targetId.toString());
            if (configurationService.getBooleanProperty("versioning.item.history.view.admin")
                && !authorizeService.isAdmin(context)) {
                return false;
            }
            Version version = versioningService.getVersion(context, versionId);
            if (version == null) {
                return true;
            }
            if (authorizeService.authorizeActionBoolean(context, version.getItem(),
                                                        restPermission.getDspaceApiActionId())) {
                return true;
            }

        } catch (SQLException e) {
            log.error(e::getMessage, e);
        }
        return false;
    }
}
