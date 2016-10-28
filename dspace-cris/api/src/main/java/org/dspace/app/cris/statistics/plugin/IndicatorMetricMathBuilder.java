package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class IndicatorMetricMathBuilder<ACO extends DSpaceObject>
        extends AIndicatorMetricSolrBuilder<ACO>
{

    public void computeMetric(Context context,
            ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed,
            Map<String, List<Double>> mapElementsValueComputed, ACO aco,
            SolrDocument doc, Integer resourceType, Integer resourceId,
            String uuid)
    {

        List<Double> elementsValueComputed = mapElementsValueComputed
                .containsKey(this.getName())
                        ? mapElementsValueComputed.get(this.getName())
                        : new ArrayList<Double>();
        if (doc != null)
        {
            for (String field : getFields())
            {
                Double count = (Double) doc.getFirstValue(field);
                if(count!=null) {
                    // sum, percentage, average, median, maximum,
                    // minimum?!
                    elementsValueComputed.add(count.doubleValue());
                }
            }

            mapElementsValueComputed.put(this.getName(), elementsValueComputed);

        }
    }

}
