/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.factory;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the authorize package, use AuthorizeServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class AuthorizeServiceFactoryImpl extends AuthorizeServiceFactory {

    @Autowired(required = true)
    private AuthorizeService authorizeService;
    @Autowired(required = true)
    private ResourcePolicyService resourcePolicyService;

    @Override
    public AuthorizeService getAuthorizeService() {
        return authorizeService;
    }

    @Override
    public ResourcePolicyService getResourcePolicyService() {
        return resourcePolicyService;
    }
}
