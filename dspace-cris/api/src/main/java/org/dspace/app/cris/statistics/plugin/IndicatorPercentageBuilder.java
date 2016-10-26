package org.dspace.app.cris.statistics.plugin;

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
            int numberOfValueComputed, int valueComputed,
            double additionalValueComputed, Integer resourceType,
            Integer resourceId, String uuid)
    {
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
