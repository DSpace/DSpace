/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.app.cris.statistics.SummaryStatBean;
import org.dspace.app.cris.statistics.SummaryStatBean.StatDataBean;
import org.dspace.app.cris.util.Researcher;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class StatsViewIndicatorsPlugin extends AStatsIndicatorsPlugin
{

    private static Logger log = Logger
            .getLogger(StatsViewIndicatorsPlugin.class);

    // set up to CRIS_DYNAMIC_TYPE_ID_START (the initial value of dynamic type
    // to simplify the xml configuration)
    private Integer resourceTypeId = CrisConstants.CRIS_DYNAMIC_TYPE_ID_START;

    private String resourceTypeString;

    @Override
    public void buildIndicator(Context context,
            ApplicationService applicationService, CrisSolrLogger statsService,
            CrisSearchService searchService, String filter)
    {
    	MetricsPersistenceService pService = new DSpace().getSingletonService(MetricsPersistenceService.class); 
        SolrQuery query = new SolrQuery(getQueryDefault());
        if (StringUtils.isNotBlank(getResourceTypeString()))
        {
            query.addFilterQuery("{!field f=resourcetype_authority}"
                    + getResourceTypeString());
        }
        else
        {
            query.addFilterQuery(
                    "{!field f=search.resourcetype}" + getResourceTypeId());
        }
        if(StringUtils.isNotBlank(filter)) {
            query.addFilterQuery(filter);
        }
        else if(StringUtils.isNotBlank(getFilterDefault())) {
            query.addFilterQuery(getFilterDefault());    
        }
        query.setFields("search.resourceid", "search.resourcetype",
                resourceTypeId == Constants.ITEM ? "handle" : "cris-uuid");
        query.setRows(Integer.MAX_VALUE);

        try
        {
            Researcher researcher = new Researcher();
            QueryResponse response = searchService.search(query);
            SolrDocumentList docList = response.getResults();
            Iterator<SolrDocument> solrDoc = docList.iterator();
            while (solrDoc.hasNext())
            {
                SolrDocument doc = solrDoc.next();
                String uuid = (String) doc
                        .getFirstValue(resourceTypeId == Constants.ITEM
                                ? "handle" : "cris-uuid");
                Integer resourceType = (Integer) doc
                        .getFirstValue("search.resourcetype");
                Integer resourceId = (Integer) doc
                        .getFirstValue("search.resourceid");
                try
                {

                    SummaryStatBean statDaily = researcher
                            .getStatSubscribeService().getStatBean(context,
                                    uuid, resourceTypeId,
                                    StatSubscription.FREQUENCY_DAILY, 1);
                    for (StatDataBean data : statDaily.getData())
                    {
                        Map<String, String> remark = new HashMap<String, String>();
                        remark.put("link", statDaily.getStatURL());
                        Date acquisitionDate = new Date();
                        buildIndicator(pService, applicationService,
                                uuid, resourceType, resourceId,
                                data.getTotalSelectedView(),
                                ConstantMetrics.STATS_INDICATOR_TYPE_VIEW,
                                null, acquisitionDate, remark);
                        
                        if(resourceTypeId==Constants.ITEM) {
                            for (String topKey : data
                                    .getPeriodAndTotalTopDownload().keySet())
                            {
                                List<Long> tmpList = data
                                        .getPeriodAndTotalTopDownload().get(topKey);
                                remark = new HashMap<String, String>();
                                remark.put("link", statDaily.getStatURL()+"&amp;type=bitstream");
                                buildIndicator(pService, applicationService,
                                        uuid, resourceType, resourceId,
                                        tmpList.get(1),
                                        ConstantMetrics.STATS_INDICATOR_TYPE_DOWNLOAD,
                                        null, acquisitionDate, remark);                                
                            }
                        }
                        else {
                            if(data.getTotalSelectedDownload()!=null && data.getTotalSelectedDownload()>0) {
                                remark = new HashMap<String, String>();
                                remark.put("link", statDaily.getStatURL()+"&amp;type=bitstream");
                                buildIndicator(pService, applicationService,
                                        uuid, resourceType, resourceId,
                                        data.getTotalSelectedDownload(),
                                        ConstantMetrics.STATS_INDICATOR_TYPE_DOWNLOAD,
                                        null, acquisitionDate, remark);
                            }
                        }
                    }

                }
                catch (SolrServerException | SQLException e)
                {
                    log.error("Error retrieving stats", e);
                }
            }
            if(isRenewMetricsCache()) {
                searchService.renewMetricsCache();
            }   
        }
        catch (SearchServiceException e)
        {
            log.error("Error retrieving documents", e);
        }
    }

    public Integer getResourceTypeId()
    {
        return resourceTypeId;
    }

    public void setResourceTypeId(Integer resourceTypeId)
    {
        this.resourceTypeId = resourceTypeId;
    }

    public String getResourceTypeString()
    {
        return resourceTypeString;
    }

    public void setResourceTypeString(String resourceTypeString)
    {
        this.resourceTypeString = resourceTypeString;
    }

}
