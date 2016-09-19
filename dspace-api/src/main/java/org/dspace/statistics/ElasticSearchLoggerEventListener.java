/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import org.apache.log4j.Logger;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Event;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * @deprecated  As of DSpace 6.0, ElasticSearch statistics are replaced by Solr statistics
 * @see org.dspace.statistics.service.SolrLoggerUsageEventListener#SolrLoggerUsageEventListener
 */
public class ElasticSearchLoggerEventListener extends AbstractUsageEventListener {

    private static Logger log = Logger.getLogger(ElasticSearchLoggerEventListener.class);

    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    @Override
    public void receiveEvent(Event event) {

        if(event instanceof UsageEvent && (((UsageEvent) event).getAction() == UsageEvent.Action.VIEW))
        {
            try{

                UsageEvent ue = (UsageEvent) event;

                EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                StatisticsServiceFactory.getInstance().getElasticSearchLoggerService().post(ue.getObject(), ue.getRequest(), currentUser);
                log.info("Successfully logged " + contentServiceFactory.getDSpaceObjectService(ue.getObject()).getTypeText(ue.getObject()) + "_" + ue.getObject().getID() + " " + ue.getObject().getName());
            }
            catch(Exception e)
            {
                log.error("General Exception: " + e.getMessage());
            }
        }
    }
}
