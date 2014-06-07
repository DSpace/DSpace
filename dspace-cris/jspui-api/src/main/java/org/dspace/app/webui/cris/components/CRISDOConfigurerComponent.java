/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components;

import org.apache.log4j.Logger;
import org.dspace.app.webui.cris.components.statistics.CrisStatDODownloadTopObjectComponent;
import org.dspace.app.webui.cris.components.statistics.CrisStatDownloadTopObjectComponent;

public class CRISDOConfigurerComponent extends
        ACRISConfigurerComponent
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(CRISDOConfigurerComponent.class);


    @Override
    protected CrisStatDownloadTopObjectComponent instanceNewCrisStatsDownloadComponent()
    {
        return new CrisStatDODownloadTopObjectComponent();
    }


}
