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

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.operation.EPersonPasswordReplaceOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * An authenticated user is allowed to view, update or delete his or her own data. This {@link RestPermissionEvaluatorPlugin}
 * implements that requirement.
 */
@Component
public class EPersonRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(EPersonRestPermissionEvaluatorPlugin.class);

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

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

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());

        EPerson ePerson = null;

        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());
            UUID dsoId = UUID.fromString(targetId.toString());

            // anonymous user
            if (ePerson == null) {
                return false;
            }

            if (dsoId.equals(ePerson.getID())) {
                return true;
            }

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public boolean hasPatchPermission(Authentication authentication, Serializable targetId, String targetType,
                                      Patch patch) {

        /**
         * First verify that the user has write permission on the eperson.
         */
        if (!hasPermission(authentication, targetId, targetType, "WRITE")) {
            return false;
        }

        List<Operation> operations = patch.getOperations();

        /**
         * The entire Patch request should be denied if it contains operations that are
         * restricted to Dspace administrators. The authenticated user is currently allowed to
         * update their own password.
         */
        for (Operation op: operations) {
            if (!op.getPath().contentEquals(EPersonPasswordReplaceOperation.OPERATION_PASSWORD_CHANGE)) {
                return false;
            }
        }

        return true;
    }

}
