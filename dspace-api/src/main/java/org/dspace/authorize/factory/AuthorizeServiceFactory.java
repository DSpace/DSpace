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
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the authorize package, use AuthorizeServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class AuthorizeServiceFactory {

    public abstract AuthorizeService getAuthorizeService();

    public abstract ResourcePolicyService getResourcePolicyService();

    public static AuthorizeServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("authorizeServiceFactory", AuthorizeServiceFactory.class);
    }
}
