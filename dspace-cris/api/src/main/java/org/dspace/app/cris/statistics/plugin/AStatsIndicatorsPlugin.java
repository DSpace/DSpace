/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.plugin;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.generator.DateValueGenerator;

public abstract class AStatsIndicatorsPlugin implements StatsIndicatorsPlugin
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(AStatsIndicatorsPlugin.class);
    
    private String name;

    private String queryDefault = "*:*";
    
    private String filterDefault;

    private boolean renewMetricsCache = true;
    
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public static void buildIndicator(MetricsPersistenceService pService, ApplicationService applicationService,
            String uuid, Integer resourceType, Integer resourceId, Object value,
            String metricsType, Date startDate, Date endDate,
            Map<String, String> maps)
    {
        CrisMetrics indicator = new CrisMetrics();

        if (maps != null)
        {
            indicator.setTmpRemark(maps);
            indicator.setRemark(indicator.buildMetricsRemark());
        }
        if (value != null)
        {
            if (value instanceof Number)
            {
                indicator.setMetricCount(((Number) value).doubleValue());
            }
            else
            {
                if (value instanceof String)
                {
                    indicator.setMetricCount(Double.parseDouble((String) value));
                }
            }
        }
        indicator.setUuid(uuid);
        indicator.setResourceTypeId(resourceType);
        indicator.setResourceId(resourceId);
        indicator.setStartDate(startDate);
        indicator.setEndDate(endDate);
        indicator.setMetricType(metricsType);

        pService.saveOrUpdate(CrisMetrics.class, indicator);
    }

    public boolean isRenewMetricsCache()
    {
        return renewMetricsCache;
    }

    public void setRenewMetricsCache(boolean renewMetricsCache)
    {
        this.renewMetricsCache = renewMetricsCache;
    }

    public String getQueryDefault()
    {
        return queryDefault;
    }
    
    public void setQueryDefault(String queryDefault)
    {
        this.queryDefault = queryDefault;
    }
    
    public String getFilterDefault()
    {
        if (StringUtils.startsWith(filterDefault, "###")
                && StringUtils.endsWith(filterDefault, "###"))
        {
            String[] firstsplitted = filterDefault.substring(3, filterDefault.length()-3).split("###", 2);
            //found field
            String field = firstsplitted[0];
            // search rule to apply
            String[] secondsplitted = firstsplitted[1].split("\\.", 2);
            //TODO extract this logic and inject from external
            String generator = secondsplitted[0];            
            String rule = secondsplitted[1];                       
            if("date".equals(generator)) {
                String value = DateValueGenerator.buildValue(rule);
                return field + ":" + value;
            }
            else {
                log.warn("ONLY generator supported for metrics is 'date'. No strategy: "+generator+" - found for rule:"+ rule);
            }
        }
        return filterDefault;
    }

    public void setFilterDefault(String filterDefault)
    {
        this.filterDefault = filterDefault;
    }
}
