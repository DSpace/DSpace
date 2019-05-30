/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.model.jdyna.DynamicObjectType;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public class StatsAggregateIndicatorsPlugin<ACO extends ACrisObject>
        extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsAggregateIndicatorsPlugin.class);

    private String field = "author_authority";

    private String type;

    private Class<ACO> crisEntityClazz;

    private Integer crisEntityTypeId;

    private boolean buildMath = false;
    
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

        List<ACO> rs = new ArrayList<ACO>();

        if (crisEntityTypeId > 1000)
        {
            DynamicObjectType dynamicType = applicationService.get(DynamicObjectType.class, crisEntityTypeId);
        	long tot = applicationService.countResearchObjectByType(dynamicType);
          	final int MAX_RESULT = 50;
        	long numpages = (tot / MAX_RESULT) + 1;
            for (int page = 1; page <= numpages; page++)
            {
        		rs.addAll((List<ACO>)applicationService.getResearchObjectPaginateListByType(dynamicType, "id", false, page, MAX_RESULT));
        	}
        }
        else
        {
        	long tot = applicationService.count(crisEntityClazz);
          	final int MAX_RESULT = 50;
        	long numpages = (tot / MAX_RESULT) + 1;
            for (int page = 1; page <= numpages; page++)
            {
        		rs.addAll(applicationService.getPaginateList(crisEntityClazz, "id", false, page, MAX_RESULT));
        	}
        }

        for (ACO rp : rs)
        {
            int itemsCited = 0;
            int citations = 0;
            List<Double> elements = new ArrayList<Double>();
            SolrQuery query = new SolrQuery();
            query.setQuery(getQueryDefault());
            if(StringUtils.isNotBlank(filter)) {
                query.addFilterQuery(filter);
            }
            else if(StringUtils.isNotBlank(getFilterDefault())) {
                query.addFilterQuery(getFilterDefault());    
            }
            query.addFilterQuery("{!field f=" + field + "}" + rp.getCrisID(),
                    "NOT(withdrawn:true)");
            query.setFields("search.resourceid", "search.resourcetype");

            query.setRows(Integer.MAX_VALUE);

            QueryResponse response = searchService.search(query);
            SolrDocumentList results = response.getResults();
            for (SolrDocument doc : results)
            {
                Integer resourceType = (Integer) doc
                        .getFirstValue("search.resourcetype");
                Integer resourceId = (Integer) doc
                        .getFirstValue("search.resourceid");

                if (resourceId != null)
                {
                    //TODO manage year (passed by filter)
                    CrisMetrics citation = pService
                            .getLastMetricByResourceIDAndResourceTypeAndMetricsType(
                                    resourceId, resourceType, type);
                    if (citation != null)
                    {
                        itemsCited++;
                        citations += citation.getMetricCount();
                        if(buildMath) {
                            elements.add(citation.getMetricCount());
                        }
                    }
                }
            }

            Date timestamp = new Date();
            buildIndicator(pService, applicationService, rp.getUuid(), rp.getType(),
                    rp.getId(), citations,
                    type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_AGGREGATE,
                    null, timestamp, null);
            buildIndicator(pService, applicationService, rp.getUuid(), rp.getType(),
                    rp.getId(), itemsCited,
                    type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_COUNT,
                    null, timestamp, null);
            
            if (buildMath)
            {
                if (elements != null && !elements.isEmpty())
                {
                    Double max = Collections.max(elements);
                    Double min = Collections.min(elements);
                    Double average = (double) citations / (double) itemsCited;
                    Double median = null;
                    Double[] elementsArray = new Double[elements.size()];
                    elementsArray = elements.toArray(elementsArray);
                    Arrays.sort(elementsArray);
                    median = IndicatorsUtils.median(elementsArray);

                    buildIndicator(pService, applicationService, rp.getUuid(),
                            rp.getType(), rp.getId(), average,
                            type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_AVERAGE,
                            null, timestamp, null);
                    buildIndicator(pService, applicationService, rp.getUuid(),
                            rp.getType(), rp.getId(), max,
                            type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_MAX,
                            null, timestamp, null);
                    buildIndicator(pService, applicationService, rp.getUuid(),
                            rp.getType(), rp.getId(), min,
                            type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_MIN,
                            null, timestamp, null);
                    buildIndicator(pService, applicationService, rp.getUuid(),
                            rp.getType(), rp.getId(), median,
                            type + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_MEDIAN,
                            null, timestamp, null);
                }
            }
        }
        
        if(isRenewMetricsCache()) {
            searchService.renewMetricsCache();
        }   
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Class<ACO> getCrisEntityClazz()
    {
        return crisEntityClazz;
    }

    public void setCrisEntityClazz(Class<ACO> crisEntityClazz)
    {
        this.crisEntityClazz = crisEntityClazz;
    }

    public Integer getCrisEntityTypeId()
    {
        return crisEntityTypeId;
    }

    public void setCrisEntityTypeId(Integer crisEntityTypeId)
    {
        this.crisEntityTypeId = crisEntityTypeId;
    }

    public boolean isBuildMath()
    {
        return buildMath;
    }

    public void setBuildMath(boolean buildMath)
    {
        this.buildMath = buildMath;
    }

}
