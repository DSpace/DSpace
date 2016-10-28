package org.dspace.app.cris.statistics.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public abstract class AIndicatorMetricSolrBuilder<ACO extends DSpaceObject>
        extends AIndicatorBuilder<ACO>
{

    @Override
    public List<String> getFields()
    {
        List<String> results = new ArrayList<String>();
        for (String input : getInputs())
        {
            results.add("crismetrics_" + input);
        }
        return results;
    }
    
    @Override
    public String getAdditionalField()
    {
        if(super.getAdditionalField()!=null) {
            return "crismetrics_" + super.getAdditionalField();
        }
        return super.getAdditionalField();
    }
    
}
