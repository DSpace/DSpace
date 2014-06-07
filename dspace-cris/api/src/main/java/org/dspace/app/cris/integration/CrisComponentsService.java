/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import java.util.Map;

public class CrisComponentsService
{
    private Map<String, ICRISComponent> components;

    public void setComponents(Map<String, ICRISComponent> components)
    {
        this.components = components;
    }

    public Map<String, ICRISComponent> getComponents()
    {
        return components;
    }
    
    
}
