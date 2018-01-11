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

import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.RestModel;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DSpacePermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;



    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

            DSpaceObjectRest dSpaceObject = (DSpaceObjectRest) targetDomainObject;
            DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(Constants.getTypeID(dSpaceObject.getType().toUpperCase()));
            int action = Constants.getActionID((permission.toString()));
            return authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObjectService.find(context, UUID.fromString(dSpaceObject.getUuid())), action, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());

            UUID dsoId = UUID.fromString(targetId.toString());
            DSpaceObjectService<DSpaceObject> dSpaceObjectService = ContentServiceFactory.getInstance().getDSpaceObjectService(Constants.getTypeID(targetType.toString()));
            DSpaceObject dSpaceObject = dSpaceObjectService.find(context, dsoId);


            int action = Constants.getActionID((String) permission);
            return authorizeService.authorizeActionBoolean(context, ePerson, dSpaceObject, action, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
