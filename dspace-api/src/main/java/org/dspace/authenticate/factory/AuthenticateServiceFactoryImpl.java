/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate.factory;

import org.dspace.authenticate.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the authenticate package, use AuthenticateServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class AuthenticateServiceFactoryImpl extends AuthenticateServiceFactory {

    @Autowired(required = true)
    private AuthenticationService authenticationService;

    @Override
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
}
