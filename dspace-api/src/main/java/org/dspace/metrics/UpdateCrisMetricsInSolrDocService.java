/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient.RemoteSolrException;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.metrics.service.CrisMetricsServiceImpl;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.utils.DSpace;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsInSolrDocService {

    private static final Logger log = LogManager.getLogger(UpdateCrisMetricsInSolrDocService.class);

    private CrisMetricsService crisMetricsService = new DSpace().getServiceManager().getServiceByName(
            CrisMetricsServiceImpl.class.getName(), CrisMetricsServiceImpl.class);

    private IndexingService crisIndexingService = new DSpace().getServiceManager().getServiceByName(
            IndexingService.class.getName(), IndexingService.class);

    public void performUpdate(Context context, DSpaceRunnableHandler handler, boolean optimize) {
        performUpdate(context, handler, optimize, null);
    }

    public void performUpdate(Context context, DSpaceRunnableHandler handler, boolean optimize, UUID resourceUuid) {
        try {
            List<CrisMetrics> metrics = resourceUuid == null
                    ? crisMetricsService.findAllLast(context,-1,-1)
                    : crisMetricsService.findLastMetricsByResourceId(context, resourceUuid, -1, -1);
            handler.logInfo("Metric update start");
            for (CrisMetrics metric : metrics) {
                try {
                    crisIndexingService.updateMetrics(context, metric);
                } catch (RemoteSolrException rse) {
                    if (StringUtils.containsIgnoreCase(rse.getMessage(), "Did not find child ID Item-")) {
                        log.error(rse.getMessage());
                    } else {
                        throw rse;
                    }
                }
            }
            handler.logInfo("Metric update end");
            if (optimize) {
                handler.logInfo("Starting solr optimization");
                crisIndexingService.optimize();
                handler.logInfo("Solr optimization performed");
            }
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}