/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
