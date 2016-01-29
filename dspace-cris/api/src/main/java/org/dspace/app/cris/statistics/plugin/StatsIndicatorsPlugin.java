package org.dspace.app.cris.statistics.plugin;

import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

public interface StatsIndicatorsPlugin
{

    void buildIndicator(Context context, ApplicationService persistenceService,
            CrisSolrLogger statsService, CrisSearchService searchService, String level) throws SearchServiceException;

    String getName();

}
