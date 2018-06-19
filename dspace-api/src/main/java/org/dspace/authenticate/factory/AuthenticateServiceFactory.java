/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.factory;

import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the authenticate package, use AuthenticateServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class AuthenticateServiceFactory {

    public abstract AuthenticationService getAuthenticationService();

    public static AuthenticateServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("authenticateServiceFactory", AuthenticateServiceFactory.class);
    }
}
