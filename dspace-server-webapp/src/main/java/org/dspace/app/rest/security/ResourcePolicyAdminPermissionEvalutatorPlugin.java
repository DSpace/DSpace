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

import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.factory.UtilServiceFactory;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 *
 * {@link RestPermissionEvaluatorPlugin} class that evaluate ADMIN permissions over a Resource Policy
 *
 * @author Mykhaylo Boychuk - (4Science.it)
 */
@Component
public class ResourcePolicyAdminPermissionEvalutatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    public static final String RESOURCE_POLICY_TYPE = "resourcepolicy";

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!DSpaceRestPermission.ADMIN.equals(restPermission) &&
            !DSpaceRestPermission.WRITE.equals(restPermission) ||
            !Strings.CI.equals(targetType, RESOURCE_POLICY_TYPE)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        try {
            DSpaceObject dso = null;
            if (NumberUtils.isNumber(targetId.toString())) {
                var id = Integer.parseInt(targetId.toString());
                dso = getDSO(context, id);
            } else {
                var uuid = UUID.fromString(targetId.toString());
                dso = getDSO(context, uuid);
            }
            return authorizeService.isAdmin(context, dso);

        } catch (SQLException e) {
            log.error(e::getMessage, e);
        }
        return false;
    }

    private DSpaceObject getDSO(Context context, int id) throws SQLException {
        ResourcePolicy resourcePolicy =  resourcePolicyService.find(context, id);
        if (resourcePolicy == null) {
            throw new ResourceNotFoundException(
                    ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME + " with id: " + id + " not found");
        }
        return resourcePolicy.getdSpaceObject();
    }

    private DSpaceObject getDSO(Context context, UUID uuid) throws SQLException {
        DSpaceObject dso = UtilServiceFactory.getInstance().getDSpaceObjectUtils().findDSpaceObject(context, uuid);
        if (dso == null) {
            throw new ResourceNotFoundException("DSpaceObject with uuid: " + uuid + " not found");
        }
        return dso;
    }

}
