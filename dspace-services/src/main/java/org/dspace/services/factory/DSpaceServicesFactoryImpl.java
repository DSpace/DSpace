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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the services package, use DSpaceServicesFactory.getInstance() to
 * retrieve an implementation
 */
public class DSpaceServicesFactoryImpl extends DSpaceServicesFactory {

    @Autowired(required = true)
    private ConfigurationService configurationService;

    @Autowired(required = true)
    private EmailService emailService;

    @Autowired(required = true)
    private EventService eventService;

    @Autowired(required = true)
    private RequestService requestService;

    @Autowired(required = true)
    private ServiceManager serviceManager;

    @Override
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    @Override
    public EmailService getEmailService() {
        return emailService;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }

    @Override
    public RequestService getRequestService() {
        return requestService;
    }

    @Override
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
