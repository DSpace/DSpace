/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.dspace.app.rest.security.DSpaceRestPermission.DELETE;
import static org.dspace.app.rest.security.DSpaceRestPermission.READ;
import static org.dspace.app.rest.security.DSpaceRestPermission.WRITE;

import java.io.Serializable;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 *
 * An authenticated user is allowed to view, update or delete his or her own
 * data. This {@link RestPermissionEvaluatorPlugin} implements that requirement.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component
public class ResearcherProfileRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    @Autowired
    private RequestService requestService;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
        DSpaceRestPermission restPermission) {

        if (!READ.equals(restPermission) && !WRITE.equals(restPermission) && !DELETE.equals(restPermission)) {
            return false;
        }

        if (!StringUtils.equalsIgnoreCase(targetType, ResearcherProfileRest.NAME)) {
            return false;
        }

        UUID id = UUIDUtils.fromString(targetId.toString());
        if (id == null) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext((HttpServletRequest) request.getServletRequest());

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        if (id.equals(currentUser.getID())) {
            return true;
        }

        return false;
    }

}
