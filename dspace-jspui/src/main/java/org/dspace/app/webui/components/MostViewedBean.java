package org.dspace.app.webui.components;

import java.util.List;

import org.dspace.discovery.configuration.DiscoveryViewConfiguration;

public class MostViewedBean
{
    private List<MostViewedItem> items;
    
    private DiscoveryViewConfiguration configuration;

    public List<MostViewedItem> getItems()
    {
        return items;
    }

    public void setItems(List<MostViewedItem> items)
    {
        this.items = items;
    }

    public DiscoveryViewConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(
            DiscoveryViewConfiguration configuration)
    {
        this.configuration = configuration;
    }
    
    
    
}
