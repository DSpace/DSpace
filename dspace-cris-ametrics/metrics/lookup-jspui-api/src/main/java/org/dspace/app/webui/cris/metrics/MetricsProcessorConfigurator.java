package org.dspace.app.webui.cris.metrics;

import java.util.Map;

public class MetricsProcessorConfigurator
{
    private Map<String, NumberFormatter> mapFormatter;

    public NumberFormatter getFormatter(String metricType)
    {
        if(!mapFormatter.containsKey(metricType)) {
            return mapFormatter.get("default");
        }
        return mapFormatter.get(metricType);
    }

    public Map<String, NumberFormatter> getMapFormatter()
    {
        return mapFormatter;
    }

    public void setMapFormatter(Map<String, NumberFormatter> mapFormatter)
    {
        this.mapFormatter = mapFormatter;
    }

}
