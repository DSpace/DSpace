/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status.factory;

import org.dspace.access.status.service.AccessStatusService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the access status package,
 * use AccessStatusServiceFactory.getInstance() to retrieve an implementation.
 */
public abstract class AccessStatusServiceFactory {

    public abstract AccessStatusService getAccessStatusService();

    public static AccessStatusServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("accessStatusServiceFactory", AccessStatusServiceFactory.class);
    }
}
