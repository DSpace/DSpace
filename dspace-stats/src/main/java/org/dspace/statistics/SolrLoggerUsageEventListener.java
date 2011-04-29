/**
 * $Id: SolrLoggerUsageEventListener.java 4745 2010-02-07 17:44:17Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/SolrLoggerUsageEventListener.java $
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
import org.dspace.statistics.util.SpiderDetector;
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
			
			    EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                SolrLogger.post(ue.getObject(), ue.getRequest(), currentUser);

			}
			catch(Exception e)
			{
				log.error(e.getMessage());
			}
		}
				
	}

}
