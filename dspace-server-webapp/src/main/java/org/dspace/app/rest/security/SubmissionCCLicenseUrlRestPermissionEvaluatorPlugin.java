/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;

import org.apache.commons.lang3.Strings;
import org.dspace.app.rest.model.SubmissionCCLicenseUrlRest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * This class will handle calls made to SubmissionCCLicenseUrlRest endpoints.
 * It will return true because access can be granted anytime it's linked from another resource
 */
@Component
public class SubmissionCCLicenseUrlRestPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {
    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {
        if (!Strings.CI.equals(SubmissionCCLicenseUrlRest.NAME, targetType)) {
            return false;
        }
        return true;
    }
}
