package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class IndicatorMetadataSumBuilder<ACO extends DSpaceObject> extends AIndicatorBuilder<ACO>
{

    public void computeMetric(Context context,
            ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed,
            ACO aco, SolrDocument doc, Integer resourceType, Integer resourceId,
            String uuid) throws Exception
    {

        Double valueComputed = mapValueComputed
                .containsKey(this.getName())
                        ? mapValueComputed.get(this.getName())
                        : 0;
                                
        DSpaceObject object = null;
        if (resourceType >= CrisConstants.RP_TYPE_ID)
        {
            object = (DSpaceObject) applicationService.getEntityByUUID(uuid);
        }
        else
        {
            object = DSpaceObject.find(context, resourceType, resourceId);

        }

        if (object != null)
        {            
            for (String metadata : getInputs())
            {
                String value = object.getMetadata(metadata);
                if (StringUtils.isNotBlank(value))
                {
                    valueComputed += Integer.parseInt(value);
                }
            }
            mapValueComputed.put(this.getName(), valueComputed);
        }
    }

}
