/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.authority;

import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.service.ApplicationService;

public class MetricImportFiller
{
    private static final Logger log = Logger.getLogger(MetricImportFiller.class);
    
    private ApplicationService applicationService;
    
    private MetricsPersistenceService metricService;
    
    private Map<String, TargetMetricFillerPlugin> plugins;
    
    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public void setPlugins(Map<String, TargetMetricFillerPlugin> plugins)
    {
        this.plugins = plugins;
    }

    public void setMetricService(MetricsPersistenceService metricService)
    {
        this.metricService = metricService;
    }

    public Map<String, TargetMetricFillerPlugin> getPlugins()
    {
        return plugins;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public MetricsPersistenceService getMetricService()
    {
        return metricService;
    }

}
