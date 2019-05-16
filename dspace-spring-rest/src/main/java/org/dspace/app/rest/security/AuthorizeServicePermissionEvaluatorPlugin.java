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

import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.util.UUIDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * DSpaceObjectPermissionEvaluatorPlugin will check persmissions based on the DSpace {@link AuthorizeService}.
 * This service will validate if the authenticated user is allowed to perform an action on the given DSpace Object
 * based on the resource policies attached to that DSpace object.
 */
@Component
public class AuthorizeServicePermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(AuthorizeServicePermissionEvaluatorPlugin.class);

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (restPermission == null) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;
        try {
            UUID dsoId = UUIDUtils.fromString(targetId.toString());
            DSpaceObjectService<DSpaceObject> dSpaceObjectService;
            try {
                dSpaceObjectService =
                        contentServiceFactory.getDSpaceObjectService(Constants.getTypeID(targetType));
            } catch (UnsupportedOperationException e) {
                // ok not a dspace object
                return false;
            }

            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

            if (dSpaceObjectService != null && dsoId != null) {
                DSpaceObject dSpaceObject = dSpaceObjectService.find(context, dsoId);

                //If the dso is null then we give permission so we can throw another status code instead
                if (dSpaceObject == null) {
                    return true;
                }

                return authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject,
                        restPermission.getDspaceApiActionId(), false);
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

}
