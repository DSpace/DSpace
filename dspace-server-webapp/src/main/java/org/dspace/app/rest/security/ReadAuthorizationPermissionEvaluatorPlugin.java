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
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.model.AuthorizationRest;
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
 * {@link RestPermissionEvaluatorPlugin} class that evaluate READ permissions for an Authorization
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class ReadAuthorizationPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(ReadAuthorizationPermissionEvaluatorPlugin.class);

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!DSpaceRestPermission.READ.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(targetType, AuthorizationRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
        try {
            // admin can always access
            if (authorizeService.isAdmin(context)) {
                return true;
            }
            EPerson ePerson = authorizationRestUtil.getEperson(context, targetId.toString());
            EPerson currUser = context.getCurrentUser();

            if (ePerson == null) {
                // everyone can check authorization for the anonymous user
                return true;
            } else {
                // anonymous user
                if (currUser != null && currUser.getID().equals(ePerson.getID())) {
                    return true;
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

}