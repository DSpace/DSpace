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

/**
* DSpace permission evaluator.
* To check if a user has permission to a target object, a list of permissionEvaluatorPlugins will be checked.
* 
* The following list of plugins exists: EPersonRestPermissionEvaluatorPlugin, AdminRestPermissionEvaluatorPlugin,
* AuthorizeServicePermissionEvaluatorPlugin, GroupRestPermissionEvaluatorPlugin
*/
@Component
public class DSpacePermissionEvaluator implements PermissionEvaluator {

    @Autowired
    private List<RestPermissionEvaluatorPlugin> permissionEvaluatorPluginList;

    /**
     *
     * @param authentication represents the user in question. Should not be null.
     * @param targetDomainObject the DSpace object for which permissions should be
     * checked. May be null in which case implementations should return false, as the null
     * condition can be checked explicitly in the expression.
     * @param permission a representation of the DSpace action as supplied by the
     * expression system. This corresponds to the DSpace action. Not null.
     * @return true if the permission is granted by one of the plugins, false otherwise
     */
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        for (RestPermissionEvaluatorPlugin permissionEvaluatorPlugin : permissionEvaluatorPluginList) {
            if (permissionEvaluatorPlugin.hasPermission(authentication, targetDomainObject, permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Alternative method for evaluating a permission where only the identifier of the
     * target object is available, rather than the target instance itself.
     *
     * @param authentication represents the user in question. Should not be null.
     * @param targetId the UUID for the DSpace object
     * @param targetType represents the DSpace object type of the target object. Not null.
     * @param permission a representation of the permission object as supplied by the
     * expression system. This corresponds to the DSpace action. Not null.
     * @return true if the permission is granted by one of the plugins, false otherwise
     */
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
