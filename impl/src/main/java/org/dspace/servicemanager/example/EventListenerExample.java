/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.servicemanager.example;

import org.apache.log4j.Logger;
import org.dspace.services.model.Event;
import org.dspace.services.model.EventListener;

/**
 * 
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision$
 */
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
