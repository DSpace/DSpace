package org.dspace.app.cris.statistics.plugin;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public class IndicatorMetadataSumBuilder extends AIndicatorBuilder
{

    public void computeMetric(Context context,
            ApplicationService applicationService,
            MetricsPersistenceService pService, int numberOfValueComputed,
            int valueComputed, Integer resourceType, Integer resourceId,
            String uuid) throws Exception
    {

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

        }
    }

}
