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
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.supervision.service.SupervisionOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * {@link RestPermissionEvaluatorPlugin} class that evaluate READ, WRITE and DELETE permissions over a WorkspaceItem
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
public class WorkspaceItemRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceItemRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    WorkspaceItemService wis;

    @Autowired
    private SupervisionOrderService supervisionOrderService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)
                && !DSpaceRestPermission.WRITE.equals(restPermission)
                && !DSpaceRestPermission.DELETE.equals(restPermission)) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(targetType, WorkspaceItemRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson ePerson = null;
        WorkspaceItem witem = null;
        try {
            ePerson = context.getCurrentUser();
            int dsoId = Integer.parseInt(targetId.toString());

            // anonymous user
            if (ePerson == null) {
                return false;
            }

            witem = wis.find(context, dsoId);

             // If the dso is null then we give permission so we can throw another status
            // code instead
            if (witem == null) {
                return true;
            }

            if (witem.getSubmitter() != null) {
                if (witem.getSubmitter().getID().equals(ePerson.getID())) {
                    return true;
                }
            }

            if (witem.getItem() != null) {
                if (supervisionOrderService.isSupervisor(context, ePerson, witem.getItem())) {
                    return authorizeService.authorizeActionBoolean(context, ePerson, witem.getItem(),
                                                                   restPermission.getDspaceApiActionId(),
                                                                   true);
                }
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
