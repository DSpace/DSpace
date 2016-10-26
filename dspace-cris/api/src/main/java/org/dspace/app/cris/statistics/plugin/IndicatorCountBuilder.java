package org.dspace.app.cris.statistics.plugin;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public class IndicatorCountBuilder
        extends AIndicatorBuilder
{

    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, int numberOfValueComputed,
            int valueComputed, Integer resourceType, Integer resourceId, String uuid)
    {
        numberOfValueComputed++;
    }

}
