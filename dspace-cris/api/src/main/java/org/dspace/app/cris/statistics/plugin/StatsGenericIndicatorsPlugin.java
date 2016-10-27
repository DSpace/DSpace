/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public class StatsGenericIndicatorsPlugin<ACO extends ACrisObject>
        extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsGenericIndicatorsPlugin.class);

    private String field = "author_authority";

    private String queryDefault = "*:*";

    private Class<ACO> crisEntityClazz;

    private Integer crisEntityTypeId;

    private List<IIndicatorBuilder> indicators;

    @Override
    public void buildIndicator(Context context,
            ApplicationService applicationService, CrisSolrLogger statsService,
            CrisSearchService searchService, String level)
            throws SearchServiceException
    {
        ServiceManager serviceManager = new DSpace().getServiceManager();
        MetricsPersistenceService pService = serviceManager.getServiceByName(
                MetricsPersistenceService.class.getName(),
                MetricsPersistenceService.class);

        // get all particular entity to get all related object from Solr
        if (crisEntityTypeId != null || crisEntityClazz != null)
        {
            List<ACO> rs = new ArrayList<ACO>();

            if (crisEntityTypeId > 1000)
            {
                rs = (List<ACO>) applicationService
                        .getResearchObjectByShortNameType(CrisConstants
                                .getEntityTypeText(crisEntityTypeId));
            }
            else
            {
                rs = applicationService.getList(crisEntityClazz);
            }

            for (ACO rp : rs)
            {
                work(context, applicationService, searchService, pService, rp);
            }
        }
        else // work direct with the solr query
        {
            work(context, applicationService, searchService, pService, null);
        }
    }

    private void work(Context context, ApplicationService applicationService,
            CrisSearchService searchService, MetricsPersistenceService pService,
            ACO rp) throws SearchServiceException
    {
        // prepare structure to store each computed value from indicator
        // alghoritm
        Map<String, Integer> mapNumberOfValueComputed = new HashMap<String, Integer>();
        Map<String, Double> mapValueComputed = new HashMap<String, Double>();
        Map<String, Double> mapAdditionalValueComputed = new HashMap<String, Double>();

        SolrQuery query = new SolrQuery();
        query.setQuery(queryDefault);
        if (rp != null)
        {
            query.addFilterQuery("{!field f=" + field + "}" + rp.getCrisID(),
                    "NOT(withdrawn:true)");
        }
        query.setFields("search.resourceid", "search.resourcetype", "cris-uuid", "handle");

        query.setRows(Integer.MAX_VALUE);

        QueryResponse response = searchService.search(query);
        SolrDocumentList results = response.getResults();

        for (SolrDocument doc : results)
        {
            if (rp == null)
            {
                // prepare structure to store each computed value from indicator
                // alghoritm
                mapNumberOfValueComputed = new HashMap<String, Integer>();
                mapValueComputed = new HashMap<String, Double>();
                mapAdditionalValueComputed = new HashMap<String, Double>();
            }

            Integer resourceType = (Integer) doc
                    .getFirstValue("search.resourcetype");
            Integer resourceId = (Integer) doc
                    .getFirstValue("search.resourceid");
            String uuid = "";
            if (resourceType != Constants.ITEM)
                uuid = (String) doc.getFirstValue("cris-uuid");
            else
            {
                uuid = (String) doc.getFirstValue("handle");
            }

            if (resourceId != null)
            {
                for (IIndicatorBuilder indicator : indicators)
                {

                    try
                    {
                        indicator.computeMetric(context, applicationService,
                                pService, mapNumberOfValueComputed, mapValueComputed,
                                resourceType, resourceId, uuid);
                        indicator.applyAdditional(context, applicationService,
                                pService, mapNumberOfValueComputed, mapValueComputed,
                                mapAdditionalValueComputed, resourceType,
                                resourceId, uuid);
                    }
                    catch (Exception ex)
                    {
                        log.error(ex.getMessage(), ex);
                    }

                }
            }
            if (rp == null)
            {
                buildIndicator(applicationService, pService,
                        mapNumberOfValueComputed, mapValueComputed,
                        mapAdditionalValueComputed, resourceType, resourceId,
                        uuid);
            }
        }
        if (rp != null)
        {
            buildIndicator(applicationService, pService,
                    mapNumberOfValueComputed, mapValueComputed,
                    mapAdditionalValueComputed, rp.getType(), rp.getId(),
                    rp.getUuid());
        }
    }

    private void buildIndicator(ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed,
            Map<String, Double> mapAdditionalValueComputed,
            Integer resourceType, Integer resourceId, String uuid)
    {
        for (IIndicatorBuilder indicator : indicators)
        {

            Date timestamp = new Date();
            if (mapAdditionalValueComputed.containsKey(indicator.getName()))
            {
                buildIndicator(pService, applicationService, uuid, resourceType,
                        resourceId,
                        mapAdditionalValueComputed.get(indicator.getName()),
                        indicator.getOutput(), null, timestamp, null);
            }
            else
            {
                if (mapValueComputed.containsKey(indicator.getName()))
                {
                    buildIndicator(pService, applicationService, uuid,
                            resourceType, resourceId,
                            mapValueComputed.get(indicator.getName()),
                            indicator.getOutput(), null, timestamp, null);
                }
                if (mapNumberOfValueComputed.containsKey(indicator.getName()))
                {
                    buildIndicator(pService, applicationService, uuid,
                            resourceType, resourceId,
                            mapNumberOfValueComputed.get(indicator.getName()),
                            indicator.getOutput()
                                    + ConstantMetrics.SUFFIX_STATS_INDICATOR_TYPE_COUNT,
                            null, timestamp, null);
                }
            }
        }
    }

    public void setQueryDefault(String queryDefault)
    {
        this.queryDefault = queryDefault;
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

    public String getQueryDefault()
    {
        return queryDefault;
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

    public List<IIndicatorBuilder> getIndicators()
    {
        return indicators;
    }

    public void setIndicators(List<IIndicatorBuilder> indicators)
    {
        this.indicators = indicators;
    }

}
