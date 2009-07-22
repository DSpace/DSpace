/**
 * $Id: EventListenerNoFilter.java 3312 2008-11-20 16:59:22Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/services/events/EventListenerNoFilter.java $
 * TestEvent.java - DS2 - Nov 20, 2008 1:17:31 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
 */

package org.dspace.services.events;

import java.util.List;
import java.util.Vector;

import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;

/**
 * This is a sample event listener for testing,
 * it does no filtering
 * 
 * @author Aaron Zeckoski (azeckoski@gmail.com) - azeckoski - 1:17:31 PM Nov 20, 2008
 */
public class EventListenerNoFilter implements EventListener {

    public List<Event> received = new Vector<Event>();
    public List<Event> getReceivedEvents() {
        return received;
    }
    public void clearReceivedEvents() {
        received.clear();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.EventListener#getEventNamePrefixes()
     */
    public String[] getEventNamePrefixes() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.EventListener#getResourcePrefix()
     */
    public String getResourcePrefix() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.EventListener#receiveEvent(org.dspace.services.model.Event)
     */
    public void receiveEvent(Event event) {
        received.add(event);
    }

}
