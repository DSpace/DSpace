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
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ResourcePolicyRestPermissionEvaluatorPlugin.class);

    public static final String RESOURCE_POLICY_PATCH = "resourcepolicy";

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

        if (!DSpaceRestPermission.ADMIN.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(targetType, RESOURCE_POLICY_PATCH)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        try {
            int resourcePolicyID = Integer.parseInt(targetId.toString());
            ResourcePolicy resourcePolicy =  resourcePolicyService.find(context, resourcePolicyID);
            if (resourcePolicy == null) {
                throw new ResourceNotFoundException(
                        ResourcePolicyRest.CATEGORY + "." + ResourcePolicyRest.NAME +
                                       " with id: " + resourcePolicyID + " not found");
            }
            DSpaceObject dso = resourcePolicy.getdSpaceObject();
            return authorizeService.isAdmin(context, dso);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

}
