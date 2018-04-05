/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.services.EventService;
import org.dspace.services.model.EventListener;

/**
 * AbstractUsageEventListener is used as the base class for listening events running
 * in the EventService.
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision: $
 */
public abstract class AbstractUsageEventListener implements EventListener {

	public AbstractUsageEventListener() {
		super();
	}

	/**
	 * Empty String[] flags to have Listener 
	 * consume any event name prefixes.
	 */
	@Override
	public String[] getEventNamePrefixes() {
		return new String[0];
	}

	/**
	 * Currently consumes events generated for
	 * all resources.
	 */
	@Override
	public String getResourcePrefix() {
		return null;
	}

	public void setEventService(EventService service) {
		if(service != null)
        {
            service.registerEventListener(this);
        }
		else
        {
            throw new IllegalStateException("EventService handed to Listener cannot be null");
        }
	
	}
	
}