/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
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
