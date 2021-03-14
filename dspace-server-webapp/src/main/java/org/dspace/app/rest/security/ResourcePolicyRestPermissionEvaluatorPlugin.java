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
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
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
 * {@link RestPermissionEvaluatorPlugin} class that evaluate READ, WRITE and DELETE permissions over a ResourcePolicy
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
@Component
public class ResourcePolicyRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(ResourcePolicyRestPermissionEvaluatorPlugin.class);

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);

        if (!DSpaceRestPermission.READ.equals(restPermission)
                && !DSpaceRestPermission.WRITE.equals(restPermission)
                && !DSpaceRestPermission.DELETE.equals(restPermission)
                || !StringUtils.equalsIgnoreCase(targetType, ResourcePolicyRest.NAME)) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getServletRequest());
        EPerson ePerson = null;

        try {
            ePerson = ePersonService.findByEmail(context, (String) authentication.getPrincipal());
            Integer dsoId = Integer.parseInt(targetId.toString());

            // anonymous user
            if (ePerson == null) {
                return false;
            }

            if (resourcePolicyService.isMyResourcePolicy(context, ePerson, dsoId)) {
                return true;
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

}
