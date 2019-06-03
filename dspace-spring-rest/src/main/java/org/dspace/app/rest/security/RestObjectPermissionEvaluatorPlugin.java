/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;

import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.patch.Patch;
import org.springframework.security.core.Authentication;

/**
 * Abstract {@link RestPermissionEvaluatorPlugin} class that contains utility methods to
 * evaluate permissions for a Rest Object.
*/
public abstract class RestObjectPermissionEvaluatorPlugin  implements RestPermissionEvaluatorPlugin {

    /**
     * Utility implementation to make the implementation of Rest Object Permission evaluator plugins more easy.
     *
     * @param authentication represents the user in question. Should not be null.
     * @param targetDomainObject the domain object for which permissions should be
     * checked. May be null in which case implementations should return false, as the null
     * condition can be checked explicitly in the expression.
     * @param permission a representation of the DSpace action as supplied by the
     * expression system. This corresponds to the DSpace action. Not null.
     * @return true if the permission is granted by one of the plugins, false otherwise
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject,
                                 Object permission) {
        BaseObjectRest restObject = (BaseObjectRest) targetDomainObject;
        return hasPermission(authentication, restObject.getId(), restObject.getType(), permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                          Object permission) {

        if (permission instanceof Patch) {
            return hasPatchPermission(authentication, targetId, targetType, (Patch) permission);
        } else {
            DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
            return hasDSpacePermission(authentication, targetId, targetType, restPermission);
        }
    }

    /**
     * Checks permissions for {@link Patch } requests. Override the default implementation in
     * plugins that require this capability.
     * @param authentication Authentication object providing user details of the authenticated user
     * @param targetId Unique identifier of the target object the user wants to view or manipulate
     * @param targetType Type of the target object the users wants to view or manipulate
     * @param patch The {@link Patch } instance
     * @return true if the user is allowed to perform the action described by the permission. False otherwise
     */
    public boolean hasPatchPermission(Authentication authentication, Serializable targetId, String targetType,
                               Patch patch) {

        return hasPermission(authentication, targetId, targetType, "WRITE");
    }

    /**
     * Plugins must implement this method to receive {@link RestPermissionEvaluatorPlugin} hasPermission
     * requests.
     * @param authentication Authentication object providing user details of the authenticated user
     * @param targetId Unique identifier of the target object the user wants to view or manipulate
     * @param targetType Type of the target object the users wants to view or manipulate
     * @param restPermission Permission object that describes the action the user wants to perform on the target object
     * @return true if the user is allowed to perform the action described by the permission. False otherwise.
     */
    public abstract boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
                                         DSpaceRestPermission restPermission);

}
