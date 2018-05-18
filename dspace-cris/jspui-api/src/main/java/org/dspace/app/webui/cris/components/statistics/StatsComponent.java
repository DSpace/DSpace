/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.content.DSpaceObject;

import it.cilea.osd.jdyna.components.IBeanSubComponent;

public abstract class StatsComponent<T extends DSpaceObject> extends ASolrStatsConfigurerComponent implements IStatsComponent
{
    public static final String DOWNLOAD = "download";
    public static final String VIEW = "view";
    
    private IBeanSubComponent bean;    
    private Class<T> targetObjectClass;
    private Class<T> relationObjectClass;
        
    public void setBean(IBeanSubComponent bean)
    {
        this.bean = bean;
    }

    public IBeanSubComponent getBean()
    {
        return bean;
    }

    public void setTargetObjectClass(Class<T> targetObjectClass)
    {
        this.targetObjectClass = targetObjectClass;
    }

    public Class<T> getTargetObjectClass()
    {
        return targetObjectClass;
    }

    public void setRelationObjectClass(Class<T> relationObjectClass)
    {
        this.relationObjectClass = relationObjectClass;
    }

    public Class<T> getRelationObjectClass()
    {
        return relationObjectClass;
    }


    
    public abstract String getMode();

}
