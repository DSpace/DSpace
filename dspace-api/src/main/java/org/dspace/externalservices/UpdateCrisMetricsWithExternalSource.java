/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
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
import org.dspace.externalservices.h_index.UpdateHindexMetrics;
import org.dspace.externalservices.scopus.UpdateScopusMetrics;
import org.dspace.externalservices.wos.UpdateWOSMetrics;
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
        crisMetricsExternalServices.put("scopus", new DSpace().getServiceManager().getServiceByName(
                                                      UpdateScopusMetrics.class.getName(),
                                                      UpdateScopusMetrics.class));
        crisMetricsExternalServices.put("wos", new DSpace().getServiceManager().getServiceByName(
                                                   UpdateWOSMetrics.class.getName(),
                                                   UpdateWOSMetrics.class));
        crisMetricsExternalServices.put("hindex", new DSpace().getServiceManager().getServiceByName(
                                                      UpdateHindexMetrics.class.getName(),
                                                      UpdateHindexMetrics.class));
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
        if (this.service.toLowerCase().equals("hindex") && StringUtils.isBlank(this.param)) {
            throw new IllegalArgumentException("The param is mandatory for " + this.service + " service");
        }
        MetricsExternalServices externalService = crisMetricsExternalServices.get(this.service.toLowerCase());
        if (externalService == null) {
            throw new IllegalArgumentException("The name of service must be provided");
        }
        try {
            performUpdate(context, externalService, service, param);
            context.complete();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    private void performUpdate(Context context, MetricsExternalServices metricsExternalServices,
            String service, String param) {
        int count = 0;
        try {
            Iterator<Item> itemIterator = findItems(context, service);
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

    private Iterator<Item> findItems(Context context, String service)
            throws SQLException, SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setMaxResults(20);
        setFilter(discoverQuery, service);
        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private void setFilter(DiscoverQuery discoverQuery, String service) {
        if ("scopus".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:Publication");
            discoverQuery.addFilterQueries("dc.identifier.doi:* OR dc.identifier.pmid:*");
        }
        if ("wos".equals(service) && StringUtils.isBlank(param)) {
            discoverQuery.addFilterQueries("relationship.type:Publication");
            discoverQuery.addFilterQueries("dc.identifier.doi:*");
        }
        if ("hindex".equals(service)) {
            discoverQuery.addFilterQueries("relationship.type:Person");
            discoverQuery.addFilterQueries("person.identifier.scopus-author-id:*");
        }
        if ("wos".equals(service) && "person".equals(param)) {
            discoverQuery.addFilterQueries("relationship.type:Person");
            discoverQuery.addFilterQueries("person.identifier.orcid:*");
        }
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
