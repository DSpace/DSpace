/**
 * $Id: EventListenerBothFilters.java 3312 2008-11-20 16:59:22Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/services/events/EventListenerBothFilters.java $
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

import org.dspace.services.model.EventListener;

/**
 * This is a sample event listener for testing,
 * it does filtering on the resource and the name
 * 
 * @author Aaron Zeckoski (azeckoski@gmail.com) - azeckoski - 1:17:31 PM Nov 20, 2008
 */
public class EventListenerBothFilters extends EventListenerNoFilter implements EventListener {

    /* (non-Javadoc)
     * @see org.dspace.services.model.EventListener#getEventNamePrefixes()
     */
    @Override
    public String[] getEventNamePrefixes() {
        // only receive events which start with aaron or test
        return new String[] {"test"};
    }

    /* (non-Javadoc)
     * @see org.dspace.services.model.EventListener#getResourcePrefix()
     */
    @Override
    public String getResourcePrefix() {
        return "test";
    }

}
