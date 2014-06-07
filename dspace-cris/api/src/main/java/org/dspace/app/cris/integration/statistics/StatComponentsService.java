/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics;

import java.util.Map;

public class StatComponentsService extends
        AStatComponentService<IStatsComponent>
{

    private Map<String, IStatsComponent> components;

    public void setComponents(Map<String, IStatsComponent> components)
    {
        this.components = components;
    }

    public Map<String, IStatsComponent> getComponents()
    {
        return components;
    }

    public IStatsComponent getSelectedObjectComponent()
    {
        return getComponents().get(_SELECTED_OBJECT);
    }

}
