package org.dspace.servicemanager.example;

import org.apache.log4j.Logger;
import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;

public class EventListenerExample implements EventListener{

	/** log4j category */
    private static Logger log = Logger
            .getLogger(EventListenerExample.class);

	public String[] getEventNamePrefixes() {
		return null;
	}

	public String getResourcePrefix() {
		return null;
	}

	public void receiveEvent(Event event) {
		log.info(event.toString());
	}
    
}
