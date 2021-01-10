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

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.metrics.service.CrisMetricsServiceImpl;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update metrics field in Solr document
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsInSolrDoc extends
             DSpaceRunnable<UpdateCrisMetricsInSolrDocScriptConfiguration<UpdateCrisMetricsInSolrDoc>> {

    private static final Logger log = LogManager.getLogger(UpdateCrisMetricsInSolrDoc.class);

    private CrisMetricsService crisMetricsService;

    private IndexingService crisIndexingService;

    protected Context context;

    @Override
    public void setup() throws ParseException {
        crisMetricsService = new DSpace().getServiceManager().getServiceByName(
                                 CrisMetricsServiceImpl.class.getName(), CrisMetricsServiceImpl.class);
        crisIndexingService = new DSpace().getServiceManager().getServiceByName(
                                  IndexingService.class.getName(), IndexingService.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateCrisMetricsInSolrDocScriptConfiguration<UpdateCrisMetricsInSolrDoc> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-metrics-in-solr",
                               UpdateCrisMetricsInSolrDocScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        try {
            performUpdate(context);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    private void performUpdate(Context context) {
        try {
            List<CrisMetrics> metrics = crisMetricsService.findAllLast(context,-1,-1);
            handler.logInfo("Update start");
            for (CrisMetrics metric : metrics) {
                crisIndexingService.updateMetrics(context, metric);
            }
            handler.logInfo("Update end");
            if (commandLine.hasOption("o")) {
                handler.logInfo("Starting solr optimization");
                crisIndexingService.optimize();
                handler.logInfo("Solr optimization performed");
            }
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }
}