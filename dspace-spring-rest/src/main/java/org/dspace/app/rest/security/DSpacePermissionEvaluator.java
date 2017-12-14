/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DSpacePermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private List<RestPermissionEvaluatorPlugin> permissionEvaluatorPluginList;


    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        for (RestPermissionEvaluatorPlugin permissionEvaluatorPlugin : permissionEvaluatorPluginList) {
            if (permissionEvaluatorPlugin.hasPermission(authentication, targetDomainObject, permission)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission) {
        for (RestPermissionEvaluatorPlugin permissionEvaluatorPlugin : permissionEvaluatorPluginList) {
            if (permissionEvaluatorPlugin.hasPermission(authentication, targetId, targetType, permission)) {
                return true;
            }
        }
        return false;
    }
}
