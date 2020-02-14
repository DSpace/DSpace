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
 * {@link RestPermissionEvaluatorPlugin} class that evaluate admin permission against a generic DSpace Object
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
@Component
public class DSpaceObjectAdminPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(DSpaceObjectAdminPermissionEvaluatorPlugin.class);

    public static final String DSPACE_OBJECT = "dspaceObject";

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!DSpaceRestPermission.ADMIN.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(targetType, DSPACE_OBJECT)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        try {
            UUID dsoUuid = UUID.fromString(targetId.toString());
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, dsoUuid);
            return authorizeService.isAdmin(context, dso);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
