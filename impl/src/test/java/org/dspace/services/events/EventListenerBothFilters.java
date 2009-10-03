/**
 * $Id: EventListenerBothFilters.java 3312 2008-11-20 16:59:22Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/services/events/EventListenerBothFilters.java $
 * TestEvent.java - DS2 - Nov 20, 2008 1:17:31 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2002-2009, The Duraspace Foundation.  All rights reserved.
 * Licensed under the Duraspace Foundation License.
 * 
 * A copy of the Duraspace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 *
 * 
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
