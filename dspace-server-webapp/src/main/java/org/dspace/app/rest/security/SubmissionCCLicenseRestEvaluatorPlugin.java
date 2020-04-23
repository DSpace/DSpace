package org.dspace.app.rest.security;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.SubmissionCCLicenseRest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class SubmissionCCLicenseRestEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                       DSpaceRestPermission restPermission) {
        if (!StringUtils.equalsIgnoreCase(SubmissionCCLicenseRest.NAME, targetType)) {
            return false;
        }
        return true;
    }
}
