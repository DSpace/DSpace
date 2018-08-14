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
import org.dspace.usage.UsageSearchEvent;
import org.dspace.usage.UsageWorkflowEvent;
import org.springframework.util.CollectionUtils;
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
			log.debug("Usage event received " + ((UsageEvent)event).getName());
			try{
			    UsageEvent ue = (UsageEvent)event;
			
			    EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                if(UsageEvent.Action.VIEW == ue.getAction()){
                	if(ue.getRequest()!=null){
                		indexer.postView(ue.getObject(), ue.getRequest(), currentUser);
                	} else {
                		indexer.postView(ue.getObject(), ue.getIp(), ue.getUserAgent(), ue.getXforwarderfor(), currentUser);
                	}
                }else
                if(UsageEvent.Action.SEARCH == ue.getAction()){
                    UsageSearchEvent usageSearchEvent = (UsageSearchEvent) ue;
                    //Only log if the user has already filled in a query !
                    if(!CollectionUtils.isEmpty(((UsageSearchEvent) ue).getQueries())){
                        indexer.postSearch(ue.getObject(), ue.getRequest(), currentUser,
                                usageSearchEvent.getQueries(), usageSearchEvent.getRpp(), usageSearchEvent.getSortBy(),
                                usageSearchEvent.getSortOrder(), usageSearchEvent.getPage(), usageSearchEvent.getScope());
                    }
                }else
                if(UsageEvent.Action.WORKFLOW == ue.getAction()){
                    UsageWorkflowEvent usageWorkflowEvent = (UsageWorkflowEvent) ue;

                    indexer.postWorkflow(usageWorkflowEvent);
                }else 
                if (UsageEvent.Action.LOGIN == ue.getAction()){
                	indexer.postLogin(ue.getObject(), ue.getRequest(), currentUser);
                }

			}
			catch(Exception e)
			{
				log.error(e.getMessage());
			}
		}
				
	}

}
