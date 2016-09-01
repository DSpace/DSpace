package org.dspace.app.cris.integration.authority;

import java.util.Map;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;

public class ItemTargetMetricFillerPlugin extends TargetMetricFillerPlugin
{

    @Override
    public void buildMetric(Context context, Item item, Metadatum m, Map<String, String> toBuildMetadata, Map<String, ACrisObject> createdObjects,
            Map<String, ACrisObject> referencedObjects,
            ApplicationService applicationService,
            MetricsPersistenceService metricService)
    {
        ItemMetadataImportFiller.buildMetric(context, item, null, m, m.getField(), getMappingDetail(), metricService);
    }

}
