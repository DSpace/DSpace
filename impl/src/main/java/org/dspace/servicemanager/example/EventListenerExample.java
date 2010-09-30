/**
 * Copyright (c) 2002-2010, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://www.dspace.org/license
 */
package org.dspace.servicemanager.example;

import org.apache.log4j.Logger;
import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;

/**
 * A sample EventListener which writes a string form of each received 
 * Event to the DSpace log.
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision$
 */
public class EventListenerExample implements EventListener{

	/** log4j category */
    private static Logger log = Logger
            .getLogger(EventListenerExample.class);

	/** @return null */
	public String[] getEventNamePrefixes() {
		return null;
	}

	/** @return null */
	public String getResourcePrefix() {
		return null;
	}

	public void receiveEvent(Event event) {
		log.info(event.toString());
	}
    
}
