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

public class CRISRPConfigurerComponent extends
        ACRISConfigurerComponent
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(CRISRPConfigurerComponent.class);


    @Override
    protected CrisStatDownloadTopObjectComponent instanceNewCrisStatsDownloadComponent()
    {
        return new CrisStatRPDownloadTopObjectComponent();
    }


}
