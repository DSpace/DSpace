/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public class StatsPercentileIndicatorsPlugin extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsPercentileIndicatorsPlugin.class);

    private String metrics;

    private List<String> fq;

    @Override
    public void buildIndicator(Context context,
            ApplicationService applicationService, CrisSolrLogger statsService,
            CrisSearchService searchService, String filter)
                    throws SearchServiceException
    {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        MetricsPersistenceService pService = serviceManager.getServiceByName(
                MetricsPersistenceService.class.getName(),
                MetricsPersistenceService.class);

        SolrQuery query = new SolrQuery();
        query.setQuery(getQueryDefault());
        query.setSort(ConstantMetrics.PREFIX_FIELD + metrics, ORDER.desc);
        if(StringUtils.isNotBlank(filter)) {
            query.addFilterQuery(filter);
        }
        else if(StringUtils.isNotBlank(getFilterDefault())) {
            query.addFilterQuery(getFilterDefault());    
        }
        if (fq != null) {
	        for(String f : fq) {
	            query.addFilterQuery(f);
	        }
        }
        query.setFields("search.resourceid", "search.resourcetype", "handle", "cris-uuid");

        query.setRows(Integer.MAX_VALUE);

        QueryResponse response = searchService.search(query);
        SolrDocumentList results = response.getResults();
        int position = 1;
        Date endDate = new Date();
        for (SolrDocument doc : results)
        {
            Integer resourceType = (Integer) doc
                    .getFirstValue("search.resourcetype");
            Integer resourceId = (Integer) doc
                    .getFirstValue("search.resourceid");

            String uuid = (String) doc
                    .getFirstValue(resourceType == Constants.ITEM
                            ? "handle" : "cris-uuid");
            long numFound = results.getNumFound();
			double percentile = ((double) position)/((double) numFound);
            
            buildIndicator(pService, applicationService, uuid, resourceType,
                    resourceId, percentile,
                    metrics + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_RANKING,
                    null, endDate, null);
            position++;
        }

        if(isRenewMetricsCache()) {
            searchService.renewMetricsCache();
        }   
    }

    public String getMetrics()
    {
        return metrics;
    }

    public void setMetrics(String metrics)
    {
        this.metrics = metrics;
    }

    public List<String> getFq()
    {
        return fq;
    }

    public void setFq(List<String> fq)
    {
        this.fq = fq;
    }

}
