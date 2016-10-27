package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public class IndicatorMathBuilder
        extends AIndicatorBuilder
{

    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed, Integer resourceType, Integer resourceId, String uuid)
    {

        List<Double> elementsValueComputed = mapElementsValueComputed
                .containsKey(this.getName())
                        ? mapElementsValueComputed.get(this.getName())
                        : new ArrayList<Double>();
                                
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
                    elementsValueComputed.add(citation.getMetricCount());
                }
            }
            
            mapElementsValueComputed.put(this.getName(), elementsValueComputed);

        }
    }

}
