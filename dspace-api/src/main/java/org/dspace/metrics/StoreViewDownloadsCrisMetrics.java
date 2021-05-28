/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.metrics;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.metrics.service.CrisMetricsServiceImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;
import org.json.JSONObject;

/**
 * Implementation of {@link DSpaceRunnable} to add CrisMetrics for view and downloads events of cris items
 *
 * @author alba aliu
 */
public class StoreViewDownloadsCrisMetrics extends
    DSpaceRunnable<StoreViewDownloadsCrisMetricsScriptConfiguration<StoreViewDownloadsCrisMetrics>> {
    private CrisMetricsService crisMetricsService;
    private static final Logger log = LogManager.getLogger(StoreViewDownloadsCrisMetrics.class);
    private Context context;
    private UpdateCrisMetricsInSolrDocService updateCrisMetricsInSolrDocService;
    @Override
    public void setup() throws ParseException {
        updateCrisMetricsInSolrDocService = new DSpace()
                                                .getServiceManager()
                                                .getServiceByName(UpdateCrisMetricsInSolrDocService.class.getName(),
                                                                  UpdateCrisMetricsInSolrDocService.class);
        crisMetricsService = new DSpace().getServiceManager()
                                         .getServiceByName(CrisMetricsServiceImpl.class.getName(),
                                                           CrisMetricsServiceImpl.class);
    }
    @Override
    @SuppressWarnings("unchecked")
    public StoreViewDownloadsCrisMetricsScriptConfiguration<StoreViewDownloadsCrisMetrics> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("store-metrics",
                StoreViewDownloadsCrisMetricsScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();
        try {
            context.turnOffAuthorisationSystem();
            performUpdateAndStorage(context);
            updateCrisMetricsInSolrDocService.performUpdate(context, handler, commandLine.hasOption("o"));
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performUpdateAndStorage(Context context) {
        TotalDownloadsAndVisitsGenerator totalDownloadsAndVisitsGenerator = new TotalDownloadsAndVisitsGenerator();
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems(context);
            handler.logInfo("Addition start");
            int countFoundItems = 0;
            int countAddedItems = 0;
            int countUpdatedItems = 0;
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                // get views and downloads for current item
                Map<String, Integer> views_downloads = totalDownloadsAndVisitsGenerator.createUsageReport(item.getID());
                countFoundItems++;
                // crismetrics savage if there are views
                if (views_downloads.get("views") > 0) {
                    //add edit cris metrics for views
                    if (createMetricObject("view", views_downloads.get("views"), item)) {
                        //if the method returns true it means that found previous metrics
                        countUpdatedItems++;
                    }
                    countAddedItems++;
                    // crismetrics savage if there are downloads
                    if (views_downloads.get("downloads") > 0) {
                        //add edit cris metrics for downloads
                        if (createMetricObject("download", views_downloads.get("downloads"), item)) {
                            //if the method returns true it means that found previous metrics
                            countUpdatedItems++;
                        }
                        countAddedItems++;
                    }
                }
                count++;
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            handler.logInfo("Found " + countFoundItems + " items");
            handler.logInfo("Added " + countAddedItems + " metrics");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (SolrServerException | IOException | AuthorizeException e) {
            e.printStackTrace();
        }
    }

    private Iterator<Item> findItems(Context context) throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("withdrawn:false");
        discoverQuery.addFilterQueries("archived:true");
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    // this method creates new metrics objects for views and downloads,
    // also returns true/false if there are/aren't previous metrics related with the item
    private boolean createMetricObject(String metricType, double metricCount, Item item)
        throws SQLException, AuthorizeException {
        boolean existentValue = false;
        // if already exists a cris metric set last flag to false
        CrisMetrics existentCrisMetrics = crisMetricsService
                                              .findLastMetricByResourceIdAndMetricsTypes(
                                                  context, metricType, item.getID());
        if (existentCrisMetrics != null) {
            //set last flag value to false
            existentCrisMetrics.setLast(false);
            existentValue = true;
        }
        // create new metrics object
        CrisMetrics newScopusMetrics = crisMetricsService.create(context, item);
        newScopusMetrics.setMetricType(metricType);
        newScopusMetrics.setMetricCount(metricCount);
        newScopusMetrics.setLast(true);
        //set remark
        JSONObject jsonRemark = new JSONObject();
        jsonRemark.put("detailUrl", "/statistics/items/" + item.getID());
        newScopusMetrics.setRemark(jsonRemark.toString());
        //if there are values one week before
        Double last_week = getDeltaPeriod(item.getID(), "week", metricType);
        if (last_week != null) {
            newScopusMetrics.setDeltaPeriod1(metricCount - last_week);
        }
        //if there are values one month before
        Double last_month = getDeltaPeriod(item.getID(), "month", metricType);
        if (last_month != null) {
            newScopusMetrics.setDeltaPeriod2(metricCount - last_month);
        }
        return existentValue;
    }

    private Double getDeltaPeriod(UUID id, String period, String type) throws SQLException {
        Optional<CrisMetrics> metricLast = crisMetricsService
                                               .getCrisMetricByPeriod(context, type, id, new Date(), period);
        //if there exist values one period ago return metric count value
        return metricLast.map(CrisMetrics::getMetricCount).orElse(null);
    }
}
