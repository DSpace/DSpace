package org.dspace.app.cris.statistics.plugin;

import java.util.List;

import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.core.Context;

public abstract class AIndicatorBuilder
        implements IIndicatorBuilder
{

    private boolean persistent;
    
    private String output;

    private String name;
    
    private List<String> inputs;

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
            int numberOfValueComputed, int valueComputed, double additionalValueComputed, Integer resourceType,
            Integer resourceId, String uuid)
    {
        //default nothing to apply
    }

    @Override
    public abstract void computeMetric(Context context, ApplicationService applicationService,
            MetricsPersistenceService pService,
            int numberOfValueComputed, int computedValue, Integer resourceType,
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
}
