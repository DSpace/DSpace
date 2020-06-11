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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class will handle Permissions for the {@link UsageReportRest} object and its calls
 *
 * @author Maria Verdonck (Atmire) on 11/06/2020
 */
@Component
public class UsageReportRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(UsageReportRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {
        if (StringUtils.equalsIgnoreCase(UsageReportRest.NAME, targetType)) {
            UUID uuidObject = UUID.fromString(StringUtils.substringBefore(targetId.toString(), "_"));
            Request request = requestService.getCurrentRequest();
            Context context = ContextUtil.obtainContext(request.getServletRequest());
            try {
                DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuidObject);
                if (dso == null) {
                    throw new IllegalArgumentException("No DSO found with this UUID: " + uuidObject);
                }
                return authorizeService.authorizeActionBoolean(context, dso, restPermission.getDspaceApiActionId());
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            return false;
        }
        return true;
    }
}
