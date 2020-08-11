/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.eperson.EPerson;
import org.dspace.services.model.Event;
import org.dspace.statistics.service.SolrLoggerService;
import org.dspace.usage.AbstractUsageEventListener;
import org.dspace.usage.UsageEvent;
import org.dspace.usage.UsageSearchEvent;
import org.dspace.usage.UsageWorkflowEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Simple SolrLoggerUsageEvent facade to separate Solr specific
 * logging implementation from DSpace.
 *
 * @author mdiggory
 */
public class SolrLoggerUsageEventListener extends AbstractUsageEventListener {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(SolrLoggerUsageEventListener.class);

    protected SolrLoggerService solrLoggerService;

    @Autowired
    public void setSolrLoggerService(SolrLoggerService solrLoggerService) {
        this.solrLoggerService = solrLoggerService;
    }

    @Override
    public void receiveEvent(Event event) {

        if (event instanceof UsageEvent) {
            log.debug("Usage event received " + event.getName());
            try {
                UsageEvent ue = (UsageEvent) event;

                EPerson currentUser = ue.getContext() == null ? null : ue.getContext().getCurrentUser();

                if (UsageEvent.Action.VIEW == ue.getAction()) {
                    if (ue.getRequest() != null) {
                        solrLoggerService.postView(ue.getObject(), ue.getRequest(), currentUser);
                    } else {
                        solrLoggerService.postView(ue.getObject(), ue.getIp(), ue.getUserAgent(), ue.getXforwardedfor(),
                                                   currentUser);
                    }
                } else if (UsageEvent.Action.SEARCH == ue.getAction()) {
                    UsageSearchEvent usageSearchEvent = (UsageSearchEvent) ue;
                    List<String> queries = new LinkedList<>();
                    queries.add(usageSearchEvent.getQuery());
                    solrLoggerService.postSearch(usageSearchEvent.getObject(), usageSearchEvent.getRequest(),
                                    currentUser, queries, usageSearchEvent.getPage().getSize(),
                                    usageSearchEvent.getSort().getBy(), usageSearchEvent.getSort().getOrder(),
                                    usageSearchEvent.getPage().getNumber(), usageSearchEvent.getScope());
                } else if (UsageEvent.Action.WORKFLOW == ue.getAction()) {
                    UsageWorkflowEvent usageWorkflowEvent = (UsageWorkflowEvent) ue;

                    solrLoggerService.postWorkflow(usageWorkflowEvent);
                }

            } catch (Exception e) {
                log.error("Error processing/logging UsageEvent {}", event.getName(), e);
            }
        }

    }

}
