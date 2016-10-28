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

import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public abstract class AStatsIndicatorsPlugin implements StatsIndicatorsPlugin
{

    private String name;

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

}
