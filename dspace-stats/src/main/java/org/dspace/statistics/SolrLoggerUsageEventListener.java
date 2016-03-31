/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.log4j.Logger;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Event;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;

/**
 * Simple SolrLoggerUsageEvent facade to separate Solr specific 
 * logging implementation from DSpace.
 * 
 * @author mdiggory
 *
 */
public class SolrLoggerUsageEventListener extends AbstractUsageEventListener {

	private static Logger log = Logger.getLogger(SolrLoggerUsageEventListener.class);
	
	public void receiveEvent(Event event) {

		if(event instanceof UsageEvent) {
		    String userName = null;
		    UsageEvent ue = (UsageEvent)event;
		    try {
			EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();
			userName = currentUser.getFullName();
			SolrLogger.post(ue.getObject(), ue.getRequest(), currentUser);
		    }
		    catch(Exception e) {
			log.error("unable to post solr usage event for user " + userName + ", request " + ue.getRequest().getRequestURI(),e);
		    }
		}
	}
}
