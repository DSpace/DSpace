/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import org.apache.log4j.Logger;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.webui.cris.components.statistics.ASolrStatsConfigurerComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatDownloadTopObjectComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatTopObjectComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatRPDownloadTopObjectComponent;
import org.dspace.browse.BrowseItem;
import org.dspace.content.DSpaceObject;

public abstract class ACRISConfigurerComponent extends
        BrowseItemConfigurerComponent
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ACRISConfigurerComponent.class);


    @Override
    public IStatsComponent getStatsDownloadComponent()
    {
        CrisStatDownloadTopObjectComponent component = instanceNewCrisStatsDownloadComponent();

        BeanFacetComponent bean = new BeanFacetComponent();
        
        bean.setFacetQuery(ASolrStatsConfigurerComponent.FILE + ":*");
        bean.setFacetField(ASolrStatsConfigurerComponent.FILE);        
        bean.setQuery(getRelationConfiguration().getQuery());
        for(String key : getTypes().keySet()) {
            bean.getSubQueries().put(key, getTypes().get(key).getFacetQuery());
        }
        
        component.setFromField("search.uniqueid");
        component.setBean(bean);
        component.setTargetObjectClass(getTarget());
        component.setRelationObjectClass(getRelationObjectClass());
        component.setApplicationService(getApplicationService());
        try
        {
            component.setRelationObjectType(CrisConstants.getEntityType(getRelationObjectClass()));
        }
        catch (InstantiationException e)
        {
            log.error(e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            log.error(e.getMessage(), e);
        }
        component.setCrisSearchService((CrisSearchService) getSearchService());
        return component;

    }


    protected abstract CrisStatDownloadTopObjectComponent instanceNewCrisStatsDownloadComponent();
    

}
