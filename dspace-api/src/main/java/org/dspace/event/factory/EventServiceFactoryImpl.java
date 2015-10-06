/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event.factory;

import org.dspace.event.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the event package, use EventServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class EventServiceFactoryImpl extends EventServiceFactory {

    @Autowired(required = true)
    private EventService eventService;

    @Override
    public EventService getEventService() {
        return eventService;
    }
}
