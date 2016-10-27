package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public interface IIndicatorBuilder
{
    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Integer resourceType, Integer resourceId, String uuid) throws Exception;
    
    public boolean isPersistent();
    public List<String> getInputs();
    
    public String getOutput();
    public String getName();
    
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, 
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> additionalValueComputed, Integer resourceType,
            Integer resourceId, String uuid);
}
