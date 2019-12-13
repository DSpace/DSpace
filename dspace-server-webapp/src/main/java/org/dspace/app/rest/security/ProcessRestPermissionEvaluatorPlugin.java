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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.Process;
import org.dspace.scripts.service.ProcessService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class will handle calls made to Process endpoints. It will return false if the current user is null or
 * if the current user is not the eperson that's attached to the process and that user isn't an admin.
 * This class is an implementation for {@link RestObjectPermissionEvaluatorPlugin} for Process endpoints.
 */
@Component
public class ProcessRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(ProcessRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private ProcessService processService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {

        if (!StringUtils.equalsIgnoreCase(targetType, ProcessRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        try {
            int processId = Integer.parseInt(targetId.toString());
            Process process = processService.find(context, processId);
            if (process == null) {
                return true;
            }
            if (!((context.getCurrentUser() == null) || (!context.getCurrentUser().equals(process.getEPerson())
                && !authorizeService.isAdmin(context)))) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
