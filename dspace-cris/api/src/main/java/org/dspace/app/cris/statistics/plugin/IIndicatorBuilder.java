package org.dspace.app.cris.statistics.plugin;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface IIndicatorBuilder<ACO extends DSpaceObject>
{
    public void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType, Integer resourceId, String uuid) throws Exception;
    
    public boolean isPersistent();
    public List<String> getInputs();
    
    public String getOutput();
    public String getName();
    
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService, 
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> additionalValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType, Integer resourceId, String uuid);

    public List<String> getFields();
    public String getAdditionalField();
}
