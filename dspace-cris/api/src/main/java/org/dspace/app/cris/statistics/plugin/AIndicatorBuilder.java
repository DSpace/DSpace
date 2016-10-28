package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public abstract class AIndicatorBuilder<ACO extends DSpaceObject>
        implements IIndicatorBuilder<ACO>
{

    private boolean persistent;
    
    private String output;

    private String name;
    
    private List<String> inputs;
    
    private List<String> fields;
    
    private String additionalField;

    
    public boolean isPersistent()
    {
        return persistent;
    }

    public void setPersistent(boolean persistent)
    {
        this.persistent = persistent;
    }

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
    }

    public List<String> getInputs()
    {
        return inputs;
    }

    public void setInputs(List<String> inputs)
    {
        this.inputs = inputs;
    }

    @Override
    public void applyAdditional(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, Double> additionalValueComputed, Map<String, List<Double>> mapElementsValueComputed, 
            ACO aco, SolrDocument doc, Integer resourceType,
            Integer resourceId, String uuid)
    {
        //default nothing to apply
    }

    @Override
    public abstract void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService,
            Map<String, Integer> mapNumberOfValueComputed,
            Map<String, Double> mapValueComputed, Map<String, List<Double>> mapElementsValueComputed,
            ACO aco, SolrDocument doc, Integer resourceType,
            Integer resourceId, String uuid) throws Exception;

    public String getName() {
        if(this.name==null) {
            return this.getOutput();
        }
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;        
    }
    
    public List<String> getFields() {
        if(this.fields==null && this.inputs!=null) {
            this.fields = this.inputs;
        }
        else if(this.fields==null) {
            this.fields = new ArrayList<String>();
        }
        return this.fields;
    }

    public void setFields(List<String> fields)
    {
        this.fields = fields;
    }
    
    public String getAdditionalField() {
        return this.additionalField;
    }

    public void setAdditionalField(String additionalField)
    {
        this.additionalField = additionalField;
    }

}
