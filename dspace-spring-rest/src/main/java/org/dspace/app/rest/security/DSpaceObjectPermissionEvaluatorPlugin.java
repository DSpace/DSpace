/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import org.dspace.app.rest.model.DSpaceObjectRest;
import org.springframework.security.core.Authentication;

public abstract class DSpaceObjectPermissionEvaluatorPlugin  implements RestPermissionEvaluatorPlugin {

    public boolean hasPermission(Authentication authentication, Object targetDomainObject,
                                 Object permission) {

        DSpaceObjectRest dSpaceObject = (DSpaceObjectRest) targetDomainObject;
        return hasPermission(authentication, dSpaceObject.getId(), dSpaceObject.getType(), permission);
    }


}
