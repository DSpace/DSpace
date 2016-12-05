/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
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
            CrisSolrLogger statsService, CrisSearchService searchService, String filter) throws SearchServiceException;

    String getName();

}
