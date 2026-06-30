/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CrisLayoutTabRest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class will handle calls made to CrisLayoutTabRest endpoints.
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Component
public class CrisLayoutTabRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    /* (non-Javadoc)
     * @see org.dspace.app.rest.security.RestObjectPermissionEvaluatorPlugin#hasDSpacePermission
     * (org.springframework.security.core.Authentication, java.io.Serializable,
     * java.lang.String, org.dspace.app.rest.security.DSpaceRestPermission)
     */
    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission restPermission) {
        if (!StringUtils.equalsIgnoreCase(CrisLayoutTabRest.NAME, targetType)) {
            return false;
        }
        return true;
    }

}
