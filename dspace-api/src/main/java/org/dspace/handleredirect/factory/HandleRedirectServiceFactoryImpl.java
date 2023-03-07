/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect.factory;

import org.dspace.handleredirect.service.HandleRedirectService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the handleredirect package, use HandleRedirectServiceFactory.getInstance() to retrieve
 * an implementation
 *
 * @author Ying Jin at rice.edu
 */
public class HandleRedirectServiceFactoryImpl extends HandleRedirectServiceFactory {

    @Autowired(required = true)
    private HandleRedirectService handleRedirectService;

    @Override
    public HandleRedirectService getHandleRedirectService() {

        return  handleRedirectService;
    }
}
