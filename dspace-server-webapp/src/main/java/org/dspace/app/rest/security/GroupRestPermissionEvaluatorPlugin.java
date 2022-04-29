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
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenticated user is allowed to view information on all the groups he or she is a member of (READ permission).
 * This {@link RestPermissionEvaluatorPlugin} implements that requirement by validating the group membership.
 */
@Component
public class GroupRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(GroupRestPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                 String targetType, DSpaceRestPermission permission) {

        //This plugin only evaluates READ access
        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)
                || Constants.getTypeID(targetType) != Constants.GROUP) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
        EPerson ePerson = context.getCurrentUser();
        try {
            UUID dsoId = UUID.fromString(targetId.toString());

            Group group = groupService.find(context, dsoId);

            // if the group is one of the special groups of the context it is readable
            if (context.getSpecialGroups().contains(group)) {
                return true;
            }

            // anonymous user
            if (ePerson == null) {
                return false;
            } else if (groupService.isMember(context, ePerson, group)) {
                return true;
            } else if (authorizeService.isCommunityAdmin(context)
                       && AuthorizeUtil.canCommunityAdminManageAccounts()) {
                return true;
            } else if (authorizeService.isCollectionAdmin(context)
                    && AuthorizeUtil.canCollectionAdminManageAccounts()) {
                return true;
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }
}
