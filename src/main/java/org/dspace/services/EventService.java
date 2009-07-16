/**
 * $Id: EventService.java 3307 2008-11-19 16:28:28Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/services/EventService.java $
 * EventService.java - DSpace2 - Oct 9, 2008 2:12:48 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services;

import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;


/**
 * Allows the creation of system events and registration of event listeners
 * for notification of system events <br/>
 * The service can be configured to log events or ignore certain events
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface EventService {

    /**
     * Fires an event immediately (does not add it to the queue)
     * 
     * @param event contains the data related to this event
     */
    public void fireEvent(Event event);

    /**
     * Queues up an event to be fired at the end of a successful request/transaction
     * 
     * @param event contains the data related to this event
     */
    public void queueEvent(Event event);

    /**
     * Register an event listener which will be notified when events occur
     * 
     * @param listener an implementation of the event listener
     */
    public void registerEventListener(EventListener listener);

}
