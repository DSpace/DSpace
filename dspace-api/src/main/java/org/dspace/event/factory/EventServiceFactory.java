/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.factory;

import org.dspace.event.service.EventService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the event package, use EventServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class EventServiceFactory {

    public abstract EventService getEventService();

    public static EventServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("eventServiceFactory", EventServiceFactory.class);
    }
}
