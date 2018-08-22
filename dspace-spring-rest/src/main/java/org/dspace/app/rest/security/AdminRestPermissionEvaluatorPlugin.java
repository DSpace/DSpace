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
 * Administrators are always allowed to perform any action on any DSpace object. This plugin will check if
 * the authenticated EPerson is an administrator of the provided target DSpace Object. If that is the case,
 * the authenticated EPerson is allowed to perform the requested action.
 */
@Component
public class AdminRestPermissionEvaluatorPlugin extends DSpaceObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(DSpaceObjectPermissionEvaluatorPlugin.class);


    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {

        //We do not check the "permission" object here because administrators are allowed to do everything

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;

        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

            if (ePerson != null) {
                //Check if user is a repository admin
                if (authorizeService.isAdmin(context, ePerson)) {
                    return true;
                } else {

                    //Check if the user is an admin of the current DSpace Object

                    UUID dsoId = UUIDUtils.fromString(targetId.toString());
                    DSpaceObjectService dSpaceObjectService = contentServiceFactory.getDSpaceObjectService(
                            Constants.getTypeID(targetType));

                    if (dSpaceObjectService != null && dsoId != null) {
                        DSpaceObject dSpaceObject = dSpaceObjectService.find(context, dsoId);

                        return authorizeService.isAdmin(context, ePerson, dSpaceObject);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
