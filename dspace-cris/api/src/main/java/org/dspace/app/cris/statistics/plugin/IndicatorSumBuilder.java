package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;

import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public class IndicatorSumBuilder
        extends AIndicatorBuilder
{

    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Integer resourceType, Integer resourceId, String uuid)
    {

        Double valueComputed = mapValueComputed
                .containsKey(this.getName())
                        ? mapValueComputed.get(this.getName())
                        : 0;
                                
        List<CrisMetrics> citations = pService
                .getLastMetricByResourceIDAndResourceTypeAndMetricsTypes(
                        resourceId, resourceType, getInputs());
        if (citations != null && !citations.isEmpty())
        {
            for (CrisMetrics citation : citations)
            {
                if (citation != null && citation.getMetricCount() > 0)
                {
                    // sum, percentage, average, median, maximum,
                    // minimum?!
                    valueComputed += citation.getMetricCount();
                }
            }
            
            mapValueComputed.put(this.getName(), valueComputed);

        }
    }

}
