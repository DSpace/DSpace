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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
@Component
public class SubscriptionAdminAndOwnerPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAdminAndOwnerPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                       String targetType, DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!DSpaceRestPermission.READ.equals(restPermission) &&
            !DSpaceRestPermission.WRITE.equals(restPermission) &&
            !DSpaceRestPermission.DELETE.equals(restPermission) ||
            !StringUtils.equals(targetType, "AdminOrOwner"))  {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        UUID dsoId = UUID.fromString(targetId.toString());
        EPerson currentUser = context.getCurrentUser();

        // anonymous user
        if (Objects.isNull(currentUser)) {
            return false;
        }

        try {
            return dsoId.equals(currentUser.getID()) || authorizeService.isAdmin(context, currentUser);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
