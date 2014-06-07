/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.statistics.IStatsComponent;
import org.dspace.app.cris.integration.statistics.IStatsDualComponent;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.content.DSpaceObject;

public abstract class CrisStatsDualComponent<T extends DSpaceObject> extends ASolrStatsConfigurerComponent<T> implements IStatsDualComponent
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(CrisStatsDualComponent.class);
    private ApplicationService applicationService;
    
    @Override
    public abstract IStatsComponent getStatsDownloadComponent();
    
    @Override
    public abstract IStatsComponent getStatsViewComponent();
    
    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public abstract Integer getRelationObjectType();
    public abstract Class<T> getRelationObjectClass();
    

}
