package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class IndicatorCountBuilder<ACO extends DSpaceObject>
        extends AIndicatorBuilder<ACO>
{

    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed,
            ACO aco, SolrDocument doc, Integer resourceType, Integer resourceId, String uuid)
    {
        Integer numberOfValueComputed = mapNumberOfValueComputed
                .containsKey(this.getName())
                        ? mapNumberOfValueComputed
                                .get(this.getName())
                        : 0;
        numberOfValueComputed++;
        mapNumberOfValueComputed.put(this.getName(), numberOfValueComputed);
    }

}
