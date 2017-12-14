/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;

import org.springframework.security.core.Authentication;

public interface RestPermissionEvaluatorPlugin {

    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission);

    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                                 Object permission);


}
