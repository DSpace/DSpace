/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.kernel.ServiceManager;
import org.dspace.metrics.scopus.UpdateScopusMetrics;
import org.dspace.metrics.scopus.UpdateScopusPersonMetrics;
import org.dspace.metrics.wos.UpdateWOSMetrics;
import org.dspace.metrics.wos.UpdateWOSPersonMetrics;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link DSpaceRunnable} to update CrisMetrics with external service as SCOPUS
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsWithExternalSource extends
       DSpaceRunnable<UpdateCrisMetricsWithExternalSourceScriptConfiguration<UpdateCrisMetricsWithExternalSource>> {

    private static final Logger log = LogManager.getLogger(UpdateCrisMetricsWithExternalSource.class);

    private Context context;

    private String service;

    private String param;

    private Map<String, MetricsExternalServices> crisMetricsExternalServices = new HashMap<>();

    @Override
    public void setup() throws ParseException {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        crisMetricsExternalServices.put("scopus",
                serviceManager.getServiceByName(UpdateScopusMetrics.class.getName(), UpdateScopusMetrics.class));
        crisMetricsExternalServices.put("wos",
                serviceManager.getServiceByName(UpdateWOSMetrics.class.getName(), UpdateWOSMetrics.class));
        crisMetricsExternalServices.put("scopus-person",
                serviceManager.getServiceByName(UpdateScopusPersonMetrics.class.getName(),
                        UpdateScopusPersonMetrics.class));
        crisMetricsExternalServices.put("wos-person",
                serviceManager.getServiceByName(UpdateWOSPersonMetrics.class.getName(), UpdateWOSPersonMetrics.class));
        this.service = commandLine.getOptionValue('s');
        this.param = commandLine.getOptionValue('p');
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
        if (service == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }
        MetricsExternalServices externalService = crisMetricsExternalServices.get(this.service.toLowerCase());
        if (externalService == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }
        try {
            context.turnOffAuthorisationSystem();
            performUpdate(context, externalService, param);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private void performUpdate(Context context, MetricsExternalServices metricsExternalServices,
            String param) {
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems(context, metricsExternalServices);
            handler.logInfo("Update start");
            int countFoundItems = 0;
            int countUpdatedItems = 0;
            while (itemIterator.hasNext()) {
                Item item = itemIterator.next();
                countFoundItems++;
                final boolean updated = metricsExternalServices.updateMetric(context, item, param);
                if (updated) {
                    countUpdatedItems++;
                }
                count++;
                if (count == 20) {
                    context.commit();
                    count = 0;
                }
            }
            context.commit();
            handler.logInfo("Found " + countFoundItems + " items");
            handler.logInfo("Updated " + countUpdatedItems + " metrics");
            handler.logInfo("Update end");
        } catch (SQLException | SearchServiceException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Iterator<Item> findItems(Context context, MetricsExternalServices service)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        for (String filter : service.getFilters()) {
            discoverQuery.addFilterQueries(filter);
        }
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

    public Map<String, MetricsExternalServices> getCrisMetricsExternalServices() {
        return crisMetricsExternalServices;
    }

    public void setCrisMetricsExternalServices(Map<String, MetricsExternalServices> crisMetricsExternalServices) {
        this.crisMetricsExternalServices = crisMetricsExternalServices;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

}
