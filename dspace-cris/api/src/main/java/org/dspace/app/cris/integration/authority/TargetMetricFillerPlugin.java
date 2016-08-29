package org.dspace.app.cris.integration.authority;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

public abstract class TargetMetricFillerPlugin
{
    
    private String type;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public abstract void buildMetric(Context context, Item item, Metadatum m,
            ApplicationService applicationService, MetricsPersistenceService metricService);
    
}
