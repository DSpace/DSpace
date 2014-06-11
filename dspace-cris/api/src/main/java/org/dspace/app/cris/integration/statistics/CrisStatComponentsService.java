/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.CrisComponentsService;
import org.dspace.app.cris.integration.ICRISComponent;



public class CrisStatComponentsService extends AStatComponentService<IStatsDualComponent>
{
    /** log4j logger */
    protected static Logger log = Logger
            .getLogger(CrisStatComponentsService.class);
    
   
    private Map<String, IStatsDualComponent> components;
    
    private CrisComponentsService crisComponentsService;
    
    private IStatsDualComponent selectedObject;
   
   
    public CrisComponentsService getCrisComponentsService()
    {
        return crisComponentsService;
    }

    public void setCrisComponentsService(CrisComponentsService crisComponentsService)
    {
        this.crisComponentsService = crisComponentsService;
    }

    public void setSelectedObject(IStatsDualComponent selectedObject)
    {
        this.selectedObject = selectedObject;
    }

    public IStatsDualComponent getSelectedObjectComponent()
    {
        return selectedObject;
    }

    public Map<String, IStatsDualComponent> getComponents()
    {
        if(this.components==null) {
            this.components = new TreeMap<String, IStatsDualComponent>();
        }
        Map<String,ICRISComponent> mapBean = crisComponentsService.getComponents();
        for(String key : mapBean.keySet()) {
            components.put(key, mapBean.get(key)); 
        }
        return components;
    }

    public void setComponents(Map<String, IStatsDualComponent> components)
    {
        this.components = components;
    }

    public IStatsDualComponent getSelectedObject()
    {        
        return selectedObject;
    }

      
}
