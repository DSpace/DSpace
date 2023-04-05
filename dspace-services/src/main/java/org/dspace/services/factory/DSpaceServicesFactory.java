/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.factory;

import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.dspace.services.EventService;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the services package, use DSpaceServicesFactory.getInstance() to retrieve an
 * implementation
 */
public abstract class DSpaceServicesFactory {
    public abstract ConfigurationService getConfigurationService();

    public abstract EmailService getEmailService();

    public abstract EventService getEventService();

    public abstract RequestService getRequestService();

    public abstract ServiceManager getServiceManager();

    public static DSpaceServicesFactory getInstance() {
        return new DSpace().getServiceManager().getServiceByName("dSpaceServicesFactory", DSpaceServicesFactory.class);
    }


}
