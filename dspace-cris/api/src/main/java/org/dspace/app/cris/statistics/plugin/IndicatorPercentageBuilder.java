package org.dspace.app.cris.statistics.plugin;

import java.util.Map;

import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public class IndicatorPercentageBuilder
        extends IndicatorSumBuilder
{

    private String metadataForRatio;

    @Override
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> mapAdditionalValueComputed, Integer resourceType,
            Integer resourceId, String uuid)
    {
        Double valueComputed = mapValueComputed
                .containsKey(this.getName())
                        ? mapValueComputed.get(this.getName())
                        : 0;
        Double additionalValueComputed = mapAdditionalValueComputed
                .containsKey(this.getName())
                        ? mapAdditionalValueComputed
                                .get(this.getName())
                        : 0;
                                
        if (valueComputed > 0)
        {
            CrisMetrics additional = pService
                    .getLastMetricByResourceIDAndResourceTypeAndMetricsType(
                            resourceId, resourceType, getMetadataForRatio());

            if (additional != null && additional.getMetricCount() > 0)
            {
                additionalValueComputed = (valueComputed
                        / additional.getMetricCount()) * 100;
            }
            
            mapValueComputed.put(this.getName(), valueComputed);
            mapAdditionalValueComputed.put(this.getName(),
                    additionalValueComputed);
        }
    }

    public String getMetadataForRatio()
    {
        return metadataForRatio;
    }

    public void setMetadataForRatio(String metadataForRatio)
    {
        this.metadataForRatio = metadataForRatio;
    }
}
