/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Interface to define a permission evaluator plugin. These plugins are used in the DSpace {@link PermissionEvaluator}
 * implementation {@link DSpacePermissionEvaluator} to check if an authenticated user has permission to perform a
 * certain action on a certain object.
 *
 * If you implement a this interface in a Spring bean, it will be automatically taken into account when evaluating
 * permissions.
 */
public interface RestPermissionEvaluatorPlugin {

    /**
     * Check in the authenticated user (provided by the {@link Authentication} object) has the specified permission on
     * the provided target object.
     * @param authentication Authentication object providing user details of the authenticated user
     * @param targetDomainObject The target object that the authenticated user wants to see or manipulate
     * @param permission Permission object that describes the action the user wants to perform on the target object
     * @return true if the user is allowed to perform the action described by the permission. False otherwise.
     */
    boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission);

    /**
     * Check in the authenticated user (provided by the {@link Authentication} object) has the specified permission on
     * the target object with the provided identifier.
     * @param authentication Authentication object providing user details of the authenticated user
     * @param targetId Unique identifier of the target object the user wants to view or manipulate
     * @param targetType Type of the target object the users wants to view or manipulate
     * @param permission Permission object that describes the action the user wants to perform on the target object
     * @return true if the user is allowed to perform the action described by the permission. False otherwise.
     */
    boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission);

}
