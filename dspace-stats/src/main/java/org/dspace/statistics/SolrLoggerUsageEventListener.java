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
import org.dspace.utils.DSpace;

/**
 * Simple SolrLoggerUsageEvent facade to separate Solr specific 
 * logging implementation from DSpace.
 * 
 * @author mdiggory
 *
 */
public class SolrLoggerUsageEventListener extends AbstractUsageEventListener {

	private static Logger log = Logger.getLogger(SolrLoggerUsageEventListener.class);
	
    DSpace dspace = new DSpace();

    SolrLogger indexer = dspace.getServiceManager().getServiceByName(SolrLogger.class.getName(),SolrLogger.class);
        
	public void receiveEvent(Event event) {

		if(event instanceof UsageEvent)
		{
			try{
			
			    UsageEvent ue = (UsageEvent)event;
			
			    EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                indexer.post(ue.getObject(), ue.getRequest(), currentUser);

			}
			catch(Exception e)
			{
				log.error(e.getMessage());
			}
		}
				
	}

}
