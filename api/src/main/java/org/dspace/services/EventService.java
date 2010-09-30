/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.services;

import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;


/**
 * Allows the creation of system events and registration of event listeners
 * for notification of system events.
 * The service can be configured to log events or ignore certain events.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface EventService {

    /**
     * Fires an event immediately (does not add it to the queue).
     * 
     * @param event contains the data related to this event
     */
    public void fireEvent(Event event);

    /**
     * Queues up an event to be fired at the end of a successful 
     * request/transaction.
     * 
     * @param event contains the data related to this event
     */
    public void queueEvent(Event event);

    /**
     * Register an event listener which will be notified when events occur.
     * 
     * @param listener an implementation of the event listener
     */
    public void registerEventListener(EventListener listener);

}
