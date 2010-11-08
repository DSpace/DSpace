/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
public final class EventListenerExample implements EventListener{

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
