/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handleredirect.factory;

import org.dspace.handleredirect.service.HandleRedirectService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the handleredirect package,
 * use HandleRedirectServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author Ying Jin at rice.edu
 */
public abstract class HandleRedirectServiceFactory {

    public abstract HandleRedirectService getHandleRedirectService();

    public static HandleRedirectServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("handleRedirectServiceFactory",
                                                       HandleRedirectServiceFactory.class);
    }
}
