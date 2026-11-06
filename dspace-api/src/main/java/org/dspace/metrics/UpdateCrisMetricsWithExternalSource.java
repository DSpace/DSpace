/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.IteratorUtils.chainedIterator;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverQuery.SORT_ORDER;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.kernel.ServiceManager;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update CrisMetrics with external service as SCOPUS
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class UpdateCrisMetricsWithExternalSource extends
       DSpaceRunnable<UpdateCrisMetricsWithExternalSourceScriptConfiguration<UpdateCrisMetricsWithExternalSource>> {

    private static final Logger log = LogManager.getLogger(UpdateCrisMetricsWithExternalSource.class);

    private Context context;

    private String serviceName;

    private String param;

    private Integer limit;

    private Map<String, MetricsExternalServices> crisMetricsExternalServices = new HashMap<>();

    private ConfigurationService configurationService;

    @Override
    public void setup() throws ParseException {

        ServiceManager serviceManager = new DSpace().getServiceManager();

        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        crisMetricsExternalServices = serviceManager.getServicesByType(MetricsExternalServices.class).stream()
            .collect(toMap(MetricsExternalServices::getServiceName, Function.identity()));

        this.serviceName = commandLine.getOptionValue('s');
        this.param = commandLine.getOptionValue('p');
        if (commandLine.hasOption('l')) {
            this.limit = Integer.valueOf(commandLine.getOptionValue('l'));
        } else {
            this.limit = getDefaultLimit();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public UpdateCrisMetricsWithExternalSourceScriptConfiguration<UpdateCrisMetricsWithExternalSource>
           getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("update-metrics",
                UpdateCrisMetricsWithExternalSourceScriptConfiguration.class);
    }

    @Override
    public void internalRun() throws Exception {
        assignCurrentUserInContext();
        assignSpecialGroupsInContext();

        if (limit < 0) {
            throw new IllegalArgumentException("The limit value must be a positive integer");
        }

        if (serviceName == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }

        MetricsExternalServices externalService = crisMetricsExternalServices.get(this.serviceName.toLowerCase());
        if (externalService == null) {
            throw new IllegalArgumentException("The name of service is unknown");
        }

        try {
            context.turnOffAuthorisationSystem();
            performUpdate(externalService);
            context.complete();
        } catch (Exception e) {
            getLogsFromMetricService(externalService);
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }

    }

    /**
     * Updates the metrics using an external service.
     */
    private void performUpdate(MetricsExternalServices metricsExternalServices) throws SQLException {

        Iterator<Item> itemIterator = findItems(metricsExternalServices);

        if (metricsExternalServices.canMultiFetch()) {
            performUpdateWithMultiFetch(metricsExternalServices, itemIterator);
        } else {
            performUpdateWithSingleFetches(metricsExternalServices, itemIterator);
        }

    }

    private Iterator<Item> findItems(MetricsExternalServices service) {

        Iterator<Item> itemsWithoutLastImport = findItemsWithoutLastImport(service);

        Iterator<Item> itemsSortedByLastImport = findItemsSortedByLastImport(service);

        Iterator<Item> chainedIterator = chainedIterator(itemsWithoutLastImport, itemsSortedByLastImport);
        return IteratorUtils.boundedIterator(chainedIterator, limit);

    }

    private Iterator<Item> findItemsWithoutLastImport(MetricsExternalServices service) {
        return findItems(service, false);
    }

    private Iterator<Item> findItemsSortedByLastImport(MetricsExternalServices service) {
        return findItems(service, true);
    }

    private Iterator<Item> findItems(MetricsExternalServices service, boolean withLastImport) {

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        for (String filter : service.getFilters()) {
            discoverQuery.addFilterQueries(filter);
        }

        String lastImportMetadataField = getLastImportMetadataField(service);
        if (withLastImport) {
            // set an upper limit to prevent items updated in the same run from being pulled out again.
            discoverQuery.setQuery(lastImportMetadataField + ": [* TO " + currentDateMinusOneSecond() + "]");
            discoverQuery.setSortField(lastImportMetadataField, SORT_ORDER.asc);
        } else {
            discoverQuery.setQuery("-" + lastImportMetadataField + ": [* TO *]");
        }

        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private String currentDateMinusOneSecond() {
        return DCDate.getCurrent().toString() + "-1SECONDS";
    }

    private String getLastImportMetadataField(MetricsExternalServices service) {
        return "dspace.lastimport." + service.getServiceName() + "_dt";
    }

    private void performUpdateWithMultiFetch(MetricsExternalServices metricsServices, Iterator<Item> itemIterator) {

        long updatedItems = metricsServices.updateMetric(context, itemIterator, param);

        getLogsFromMetricService(metricsServices);
        handler.logInfo("Updated " + updatedItems + " metrics");
        handler.logInfo("Update end");

    }

    private void performUpdateWithSingleFetches(MetricsExternalServices metricsServices,
        Iterator<Item> itemIterator) throws SQLException {

        handler.logInfo("Update start");

        int count = 0;
        int countFoundItems = 0;
        int countUpdatedItems = 0;

        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            countFoundItems++;
            final boolean updated = metricsServices.updateMetric(context, item, param);
            if (updated) {
                countUpdatedItems++;
            }

            metricsServices.setLastImportMetadataValue(context, item);

            count++;
            if (count == 20) {
                context.commit();
                count = 0;
            }
        }

        context.commit();
        getLogsFromMetricService(metricsServices);
        handler.logInfo("Found " + countFoundItems + " items");
        handler.logInfo("Updated " + countUpdatedItems + " metrics");
        handler.logInfo("Update end");
    }

    private Integer getDefaultLimit() {
        return configurationService.getIntProperty("metrics.update-metrics.limit", Integer.MAX_VALUE);
    }

    private void assignCurrentUserInContext() throws SQLException {
        context = new Context();
        UUID uuid = getEpersonIdentifier();
        if (uuid != null) {
            EPerson ePerson = EPersonServiceFactory.getInstance().getEPersonService().find(context, uuid);
            context.setCurrentUser(ePerson);
        }
    }

    private void assignSpecialGroupsInContext() throws SQLException {
        for (UUID uuid : handler.getSpecialGroups()) {
            context.setSpecialGroup(uuid);
        }
    }

    private void getLogsFromMetricService(MetricsExternalServices metricsServices) {
        List<String> metricLogger = metricsServices.getLogs();
        if (metricLogger != null) {
            metricLogger.forEach(message -> handler.logInfo(message));
        }
    }
}
