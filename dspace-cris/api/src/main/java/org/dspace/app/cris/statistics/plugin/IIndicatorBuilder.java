package org.dspace.app.cris.statistics.plugin;

import java.util.List;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public interface IIndicatorBuilder
{
    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, int numberOfValueComputed,
            int computedValue, Integer resourceType, Integer resourceId, String uuid) throws Exception;
    
    public boolean isPersistent();
    public List<String> getInputs();
    
    public String getOutput();
    public String getName();
    
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, 
            int numberOfValueComputed, int valueComputed, double additionalValueComputed, Integer resourceType,
            Integer resourceId, String uuid);
}
