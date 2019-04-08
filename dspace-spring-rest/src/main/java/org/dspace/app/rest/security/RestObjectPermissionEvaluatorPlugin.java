/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.app.rest.model.BaseObjectRest;
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
    public boolean hasPermission(Authentication authentication, Object targetDomainObject,
                                 Object permission) {
        BaseObjectRest restObject = (BaseObjectRest) targetDomainObject;
        return hasPermission(authentication, restObject.getId(), restObject.getType(), permission);
    }


}
