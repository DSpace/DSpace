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
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.operation.DSpaceObjectMetadataPatchUtils;
import org.dspace.app.rest.repository.patch.operation.EPersonPasswordAddOperation;
import org.dspace.app.rest.repository.patch.operation.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenticated user is allowed to view, update or delete their own data. This {@link RestPermissionEvaluatorPlugin}
 * implements that requirement.
 */
@Component
public class EPersonRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                 String targetType, DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission)
                && !DSpaceRestPermission.WRITE.equals(restPermission)
                && !DSpaceRestPermission.DELETE.equals(restPermission)) {
            return false;
        }
        if (Constants.getTypeID(targetType) != Constants.EPERSON) {
            return false;
        }
        if (targetId == null) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson ePerson = null;

        ePerson = context.getCurrentUser();
        UUID dsoId = UUID.fromString(targetId.toString());

        // anonymous user
        try {
            if (ePerson == null) {
                return false;
            } else if (dsoId.equals(ePerson.getID())) {
                return true;
            } else if (authorizeService.isCommunityAdmin(context)
                && AuthorizeUtil.canCommunityAdminManageAccounts()) {
                return true;
            } else if (authorizeService.isCollectionAdmin(context)
                && AuthorizeUtil.canCollectionAdminManageAccounts()) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e::getMessage, e);
        }


        return false;
    }

    @Override
    public boolean hasPatchPermission(Authentication authentication, Serializable targetId, String targetType,
                                      Patch patch) {

        List<Operation> operations = patch.getOperations();
        // If it's a password replace action, we can allow anon through provided that there's a token present
        Request currentRequest = requestService.getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest httpServletRequest = currentRequest.getHttpServletRequest();
            if (!operations.isEmpty()
                && StringUtils.equalsIgnoreCase(operations.get(0).getOp(), PatchOperation.OPERATION_ADD)
                && StringUtils.equalsIgnoreCase(operations.get(0).getPath(),
                EPersonPasswordAddOperation.OPERATION_PASSWORD_CHANGE)
                && StringUtils.isNotBlank(httpServletRequest.getParameter("token"))) {
                return true;
            }
        }
        /**
         * First verify that the user has write permission on the eperson.
         */
        if (!hasPermission(authentication, targetId, targetType, "WRITE")) {
            return false;
        }


        /**
         * The entire Patch request should be denied if it contains operations that are
         * restricted to Dspace administrators. The authenticated user is currently allowed to
         * update their own password and their own metadata.
         */
        for (Operation op: operations) {
            if (!(op.getPath().contentEquals(EPersonPasswordAddOperation.OPERATION_PASSWORD_CHANGE)
                || (op.getPath().startsWith(DSpaceObjectMetadataPatchUtils.OPERATION_METADATA_PATH)))) {
                return false;
            }
        }

        return true;
    }

}
