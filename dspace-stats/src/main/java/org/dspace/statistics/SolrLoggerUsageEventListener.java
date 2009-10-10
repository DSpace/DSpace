/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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

		if(event instanceof UsageEvent)
		{
			try{
			
			UsageEvent ue = (UsageEvent)event;
			
			String ip = null;
			
	        if(SolrLogger.isUseProxies())
	            ip = ue.getRequest().getHeader("X-Forwarded-For");
	        
	        if(ip == null || ip.equals(""))
	            ip = ue.getRequest().getRemoteAddr();

	        EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

	        SolrLogger.post(ue.getObject(), ip, currentUser);
			
	    	
			}
			catch(Exception e)
			{
				log.error(e.getMessage());
			}
		}
				
	}

}
